/**
 * Created by myth on 6/16/15.
 */

@import play.i18n._

// We need the map object and geocoder to be on global scope
var map;
var geocoder;

// The main MapView module
var MapView = (function ($) {
    /* Private fields */
    var DEBUG = true;
    var FIRST_RUN = true;
    var UPDATE_INTERVAL = 15000;
    var ACTIVE_ALARM = null;

    var fieldOperatorLocations = [];
    var alarms = [];
    var fieldOperatorMarkers = [];
    var alarmMarkers = [];

    var allAlarmsPeriodicUpdateTimer = null;

    var highlightedOperator = null;

    /* Private methods here */

    // Helper method that sets up button listeners and map initialization
    var bindButtons = function () {
        $('#open-map-button').on('click', function (e) {
            e.preventDefault();
            $('#main-dashboard').hide();
            $('#map-dashboard').show();

            /*
            Since the div containing the map is in display:none mode, the map will not render properly automatically.
            Thus, we have to trigger map initialization when the button is clicked, and the parent div has been
            expanded to visible mode by the browser, and is renderable. Otherwise, the Google Maps library cannot
            render, as the containing div has a height and width of 0.
             */
            if (FIRST_RUN) {
                // Scale the map container to the height of the screen
                $('#map-container').css('height', $(window).height());
                // Basic map configuration
                var mapOptions = {
                    center: new google.maps.LatLng(63.4137195, 10.4090516),
                    zoom: 13
                };
                // Initialize the map
                map = new google.maps.Map(document.getElementById("map-container"), mapOptions);
            }

            // Schedule frequent updating of current positions of operatives
            MapView.getAllCurrentPositions();
            allAlarmsPeriodicUpdateTimer = setInterval(MapView.getAllCurrentPositions, UPDATE_INTERVAL);

            // Fetch alarm incident locations. The module method will decide what to do itself based on
            // the value of ACTIVE_ALARM
            MapView.getAlarmLocations(ACTIVE_ALARM);

            // Flag firstRun as false to prevent having to re-initialize the map every time
            if (FIRST_RUN) FIRST_RUN = false;
        });
        // Regular close map button in Map View
        $('#close-map-button').on('click', function (e) {
            e.preventDefault();
            $('#map-dashboard').hide();
            $('#main-dashboard').show();
            // Reset any potential active alarm and stop the periodic update of locations and markers
            // since the map is in a closed state
            ACTIVE_ALARM = null;
            clearInterval(allAlarmsPeriodicUpdateTimer);
        });
        // Select Mobile caretaker button in openAlarms dashboard
        $('#dispatch-map-button').on('click', function (e) {
            e.preventDefault();
            ACTIVE_ALARM = Alarms.gui.getCurrentSelectedAlarmIndex();
            $('#open-map-button').click();
        });
        bindAssignButtons();
    };

    // Helper method that only binds assign alarm buttons in the sidebar
    var bindAssignButtons = function () {
        // Assign active alarm button in sidebar
        $('.assign-map-button').on('click', function (e) {
            e.preventDefault();
            Actions.saveAndFollowupAtClosing({
                type: 'mobileCareTaker',
                id: Number($(this).parent().attr('id').replace('field-operator', ''))
            })
            $('#close-map-button').click();
        });
    }

    // Helper method that generates custom Image icons for map markers
    var createMarkerIcon = function (imageUrl) {
        return {
            url: imageUrl,
            size: new google.maps.Size(256, 256),
            origin: new google.maps.Point(0, 0),
            anchor: new google.maps.Point(20, 20),
            scaledSize: new google.maps.Size(40, 40)
        }
    };

    // Helper method that generates marker tooltips for map markers
    var createMarkerTooltip = function (marker, title, text) {
        var tooltip = '<div id="tooltip-' + marker.fieldOperator + '">' +
                '<h4>' + title + '<h4><p>' + text + '</p></div>';
        var info = new google.maps.InfoWindow({
            content: tooltip
        });
        google.maps.event.addListener(marker, 'click', function () {
           info.open(map, marker);
        });
    };

    // Helper method that sets up an click event binding from a map marker to sidebar list items
    var bindFieldOperatorMarkerHighlight = function (marker) {
        google.maps.event.addListener(marker, 'click', function () {
            if (highlightedOperator !== null) {
                highlightedOperator.removeClass('active');
            }
            highlightedOperator = $('#field-operator' + marker.fieldOperator);
            highlightedOperator.addClass('active');
        });
    }

    // Helper method that draws field operator markers on the map
    var updateFieldOperatorMarkers = function () {
        // Clear the markers cache
        for (var i = 0; i < fieldOperatorMarkers.length; i++) {
            fieldOperatorMarkers[i].setMap(null);
        }
        fieldOperatorMarkers = [];
        // Add all field operators
        for (var i = 0; i < fieldOperatorLocations.length; i++) {
            var pos = new google.maps.LatLng(fieldOperatorLocations[i].latitude, fieldOperatorLocations[i].longitude);
            var icon = createMarkerIcon('/assets/images/map/ambulance.png');
            var marker = new google.maps.Marker({
                position: pos,
                map: map,
                icon: icon,
                title: fieldOperatorLocations[i].username
            });
            // Add the operator ID for future mapping reference
            marker.fieldOperator = fieldOperatorLocations[i].id;

            // Create a tooltip for the marker and add an event listener (Not used due to malformed anchoring)
            // createMarkerTooltip(marker, fieldOperatorLocations[i].username, 'Dummy status data...');

            // Add marker click event highlighting
            bindFieldOperatorMarkerHighlight(marker);

            fieldOperatorMarkers.push(marker);
        }
    };

    // Helper method that draws alarm incident locations on the map
    var updateAlarmMarkers = function () {
        // Clear the markers cache
        for (var i = 0; i < alarmMarkers.length; i++) {
            alarmMarkers[i].setMap(null);
        }
        alarmMarkers = [];
        // Add all alarm markers
        for (var i = 0; i < alarms.length; i ++) {
            var pos = new google.maps.LatLng(alarms[i].latitude, alarms[i].longitude);
            var icon = createMarkerIcon('/assets/images/map/incident.png');
            var marker = new google.maps.Marker({
                position: pos,
                map: map,
                icon: icon,
                title: alarms[i].type
            });
            // Add the alarm ID for future mapping reference
            marker.alarmId = alarms[i].id;

            alarmMarkers.push(marker);
        }
    }

    // Helper method that updates the sidebar
    var updateSidebar = function () {
        var html = '<ul class="map-field-operator">';
        for (var i = 0; i < fieldOperatorLocations.length; i++) {
            html += '<li id="field-operator' + fieldOperatorLocations[i].id + '"><strong>' +
                fieldOperatorLocations[i].username + '</strong><br><small>' +
                new Date(fieldOperatorLocations[i].timestamp) + '</small><br>' +
                '<i>Trygghetspatrulje</i><br>Oppdrag: <span style="color: green;"><strong>LEDIG</strong></span><br>';

            if (ACTIVE_ALARM !== null) {
                html += '<button class="btn btn-default pull-right assign-map-button">@Messages.get("actions.button.map.assign")</button>';
            }

            html += '</li>';
        }
        html += '</ul>'

        $('#map-sidebar-fieldoperators').html(html);
        if (ACTIVE_ALARM !== null) bindAssignButtons();
    }

    /* Public methods inside return object */

    return {
        init: function () {
            // Add click listeners to hide and show map buttons
            bindButtons();
            // Initialize the GeoCoder so it may be used during incident registration
            geocoder = new google.maps.Geocoder();
        },
        getAllCurrentPositions: function () {
            $.getJSON('/location/current', function (data) {
                if (DEBUG) console.log("Fetched current locations.", data);
                fieldOperatorLocations = data.users;
                updateFieldOperatorMarkers();
                updateSidebar();
            });
        },
        getAlarmLocations: function (id) {
            if (id === null || id === undefined) {
                // TODO: Fetch all alarm locations and do proper things with the data
            } else {
                $.getJSON('/alarm/' + id, function (data) {
                    if (DEBUG) console.log("Fetched active alarm.", data);
                    alarms = [];
                    data.latitude = 63.419720;
                    data.longitude = 10.399124;
                    alarms.push(data);
                    updateAlarmMarkers();
                });
            }
        }
    }
})(jQuery);

// On page load complete
$(document).ready(function () {
    MapView.init();
})