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

    var fieldOperatorLocations = [];
    var alarms = [];
    var fieldOperatorMarkers = [];
    var alarmMarkers = [];

    // Used when selecting subsets of all open alarms for display in map view
    var alarmCache = null;

    var fieldOperatorLocationPeriodicUpdateTimer = null;

    var highlightedOperator = null;

    /* Private methods here */

    // Helper method that handles initialization, drawing and updating map
    var showMap = function () {
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
            google.maps.event.addListener(map, 'click', function (e) {
                MapView.resetSelectedAlarmsOnDisplay();
            });
        }

        // Schedule frequent updating of current positions of operatives
        MapView.getAllCurrentPositions();
        fieldOperatorLocationPeriodicUpdateTimer = setInterval(MapView.getAllCurrentPositions, UPDATE_INTERVAL);

        // Fetch alarm incident locations. The module method will decide what to do itself based on
        // the value of ACTIVE_ALARM
        MapView.getAlarmLocations(Alarms.getActiveAlarm());

        // Flag firstRun as false to prevent having to re-initialize the map every time
        if (FIRST_RUN) FIRST_RUN = false;
    };

    // Helper method that sets up button listeners and map initialization
    var bindButtons = function () {
        $('#open-map-button').on('click', function (e) {
            e.preventDefault();
            // Since the regular open map button was pressed, we deselect any active alarm
            if (Alarms.getActiveAlarm() !== null) Alarms.getActiveAlarm().deselect();
            showMap();
        });
        // Regular close map button in Map View
        $('#close-map-button').on('click', function (e) {
            e.preventDefault();
            $('#map-dashboard').hide();
            $('#main-dashboard').show();
            clearInterval(fieldOperatorLocationPeriodicUpdateTimer);
        });
        // Select Mobile caretaker button in openAlarms dashboard
        $('#dispatch-map-button').on('click', function (e) {
            e.preventDefault();
            showMap();
        });
        bindAssignButtons();
    };

    // Helper method that only binds assign alarm buttons in the sidebar
    var bindAssignButtons = function () {
        // Assign active alarm button in sidebar
        $('.assign-map-button').on('click', function (e) {
            e.preventDefault();
            var payload = {
                type: 'mobileCareTaker',
                id: Number($(this).parent().attr('id').replace('field-operator', ''))
            }
            Actions.saveAndFollowupAtClosing(payload);
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
        google.maps.event.addListener(marker, 'click', function (e) {
            if (highlightedOperator !== null) {
                highlightedOperator.removeClass('active');
            }
            highlightedOperator = $('#field-operator' + marker.fieldOperator.id);
            highlightedOperator.addClass('active');

            // If we have no active alarm assignment state, activate the fieldoperator to incident indicators
            if (Alarms.getActiveAlarm() === null) {
                // Swap out alarms on display with alarms assigned to this field operator
                MapView.displayCurrentAssignmentsForFieldOperator(marker.fieldOperator);
            }
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
            marker.fieldOperator = fieldOperatorLocations[i];

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

    // Helper method that updates the sidebar with data about field operators, their types and assignments
    var updateSidebar = function () {

        // Sort the fieldOperatorLocations by number of assignments
        fieldOperatorLocations.sort(function (a, b) {
            if (a.assignedAlarms.length < b.assignedAlarms.length) return -1;
            else if (a.assignedAlarms.length > b.assignedAlarms.length) return 1;
            return 0;
        });

        // Time to draw the list items
        var html = '<ul class="map-field-operator">';
        for (var i = 0; i < fieldOperatorLocations.length; i++) {
            html += '<li id="field-operator' + fieldOperatorLocations[i].id + '"><strong>' +
                fieldOperatorLocations[i].username + '</strong><br><small>' +
                new Date(fieldOperatorLocations[i].timestamp) + '</small><br>' +
                '<i>@Messages.get("map.sidebar.type.mobilecaretaker")</i><br>@Messages.get("map.sidebar.assignments"): ';

            // Display amount of assigned alarms
            if (fieldOperatorLocations[i].assignedAlarms.length === 0) {
                html += '<span style="color: green;"><strong>@Messages.get("map.sidebar.assignments.available")';
            } else {
                html += '<span style="color: blue;"><strong>' + fieldOperatorLocations[i].assignedAlarms.length;
            }
            html += '</strong></span><br>';

            // Add the assign button if we have an active alarm to dispatch
            if (Alarms.getActiveAlarm() !== null) {
                html += '<button class="btn btn-default assign-map-button">@Messages.get("actions.button.map.assign")</button>';
            }

            html += '</li>';
        }
        html += '</ul>';

        $('#map-sidebar-fieldoperators').html(html);
        if (Alarms.getActiveAlarm() !== null) bindAssignButtons();
    };

    /* Public methods inside return object */

    return {
        init: function () {
            // Add click listeners to hide and show map buttons
            bindButtons();
            // Initialize the GeoCoder so it may be used during incident registration
            geocoder = new google.maps.Geocoder();
        },

        getAllCurrentPositions: function (str, fn) {
            $.getJSON('/location/current', function (data) {
                if (DEBUG) console.log("Fetched current locations.", data);
                fieldOperatorLocations = data.users;
                updateFieldOperatorMarkers();
                updateSidebar();
            });
        },

        getAlarmLocations: function (alarm) {
            if (alarm === null || alarm === undefined) {
                $.getJSON('/alarm/allOpen', function (data) {
                    if (DEBUG) console.log("Fetched all open alarms", data);
                    alarms = [];
                    for (var i = 0; i < data.total; i++) {
                        alarms.push(data.alarms[i]);
                    }
                    // Shallow clone the array into cache
                    alarmCache = alarms.slice(0);
                    updateAlarmMarkers();
                });
            } else {
                $.getJSON('/alarm/' + alarm.id, function (data) {
                    if (DEBUG) console.log("Fetched active alarm.", data);
                    alarms = [];
                    alarms.push(data);
                    // Shallow clone the array into cache
                    alarmCache = alarms.slice(0);
                    updateAlarmMarkers();
                });
            }
        },

        // When a specific field operator is clicked in the map view we only show alarms assigned to that operator.
        displayCurrentAssignmentsForFieldOperator: function (fieldOperator) {
            alarms = [];
            for (var i = 0; i < alarmCache.length; i++) {
                var alarm = alarmCache[i];
                if (alarm.mobileCareTaker !== null && alarm.mobileCareTaker.id === fieldOperator.id) {
                    alarms.push(alarm);
                }
            }
            if (DEBUG) console.log("FieldOp marker clicked, selected subset:", alarms);
            updateAlarmMarkers();
        },

        // Resetting the view from subset to complete set of alarm markers
        resetSelectedAlarmsOnDisplay: function () {
            // If currently displayed alarms differ from that of the cache, we update current display set
            // and redraw the markers
            if (alarmCache.length !== alarms.length) {
                // Shallow clone the contents of the cache back into the display array
                alarms = alarmCache.slice(0);
                updateAlarmMarkers();
            }
        },

        // Takes in a canonical address and attempts to geocode it. Callback is invoked with null as argument
        // if geocoding failed, otherwise, a LatLng object is returned.
        convertAddressToLatLng: function (address, callback) {
            var adr = {'address': address};
            geocoder.geocode(adr, function (results, status) {
                if (DEBUG) console.log(results[0].geometry.location);
                if (status === google.maps.GeocoderStatus.OK) {
                    callback({
                        latitude: results[0].geometry.location.A,
                        longitude: results[0].geometry.location.F
                    });
                } else {
                    callback(null);
                }
            });
        }
    }
})(jQuery);

// On page load complete
$(document).ready(function () {
    MapView.init();
})