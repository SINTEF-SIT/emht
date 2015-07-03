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
    var fieldOperatorLocationCache = null;

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
            showMap();
        });
        // Regular close map button in Map View
        $('#close-map-button').on('click', function (e) {
            e.preventDefault();
            $('#map-dashboard').hide();
            $('#main-dashboard').show();
            clearInterval(fieldOperatorLocationPeriodicUpdateTimer);
            fieldOperatorLocationPeriodicUpdateTimer = null;
            if (Alarms.getActiveAlarm() !== null) Alarms.getActiveAlarm().deselect();
        });
        bindAssignButtons();
        bindCallButtons();
    };

    // Helper method that only binds assign alarm buttons in the sidebar
    var bindAssignButtons = function () {
        // Assign active alarm button in sidebar
        $('.assign-map-button').on('click', function (e) {
            e.preventDefault();
            var payload = {
                type: 'mobileCareTaker',
                id: Number($(this).parent().attr('id').replace('field-operator', ''))
            };
            Actions.saveAndFollowupAtClosing(payload);

            // Close and clear
            $('#map-dashboard').hide();
            $('#main-dashboard').show();
            clearInterval(fieldOperatorLocationPeriodicUpdateTimer);
            fieldOperatorLocationPeriodicUpdateTimer = null;
        });
    };

    // Helper method that only binds call buttons in the sidebar
    var bindCallButtons = function () {
        $('.map-field-operator').find('.dispatch-ring-btn').on('click', function (e) {
            e.preventDefault();
            alert('Calling Field Operator...');
        })
    };

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
                highlightedOperator.children('.assignmentQueue').hide()
            }
            highlightedOperator = $('#field-operator' + marker.fieldOperator.id);
            highlightedOperator.addClass('active');
            highlightedOperator.children('.assignmentQueue').show();

            // Swap out alarms on display with alarms assigned to this field operator
            MapView.displayCurrentAssignmentsForFieldOperator(marker.fieldOperator);
        });
    };

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
            var icon;
            if (alarms[i].mobileCareTaker !== null) icon = createMarkerIcon('/assets/images/map/incident.png');
            else icon = createMarkerIcon('/assets/images/map/incident_unassigned.png');
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
    };

    // Helper method that updates the sidebar with data about field operators, their types and assignments
    var updateSidebar = function () {
        var activeAlarm = Alarms.getActiveAlarm();

        // Sort the fieldOperatorLocations by number of assignments
        fieldOperatorLocationCache.sort(function (a, b) {
            if (a.assignedAlarms.length < b.assignedAlarms.length) return -1;
            else if (a.assignedAlarms.length > b.assignedAlarms.length) return 1;
            return 0;
        });

        // Time to draw the list items
        var html = '<ul class="map-field-operator">';
        for (var i = 0; i < fieldOperatorLocationCache.length; i++) {
            html += '<li id="field-operator' + fieldOperatorLocationCache[i].id + '"><strong>' +
                fieldOperatorLocationCache[i].username + '</strong> ' +
                '(<i>@Messages.get("map.sidebar.type.mobilecaretaker")</i>)<br><small>' +
                new Date(fieldOperatorLocationCache[i].timestamp) + '</small><br>' +
                '@Messages.get("map.sidebar.assignments"): ';

            // Display amount of assigned alarms
            if (fieldOperatorLocationCache[i].assignedAlarms.length === 0) {
                html += '<span style="color: green;"><strong>@Messages.get("map.sidebar.assignments.available")';
            } else {
                html += '<span style="color: blue;"><strong>' + fieldOperatorLocationCache[i].assignedAlarms.length;
            }
            html += '</strong></span><br>';
            html += '<div class="assignmentQueue" style="display: none;">';
            fieldOperatorLocationCache[i].assignedAlarms.sort(function (a, b) {
                if (a.dispatchingTime < b.dispatchingTime) return -1;
                else if (a.dispatchingTime > b.dispatchingTime) return 1;
                return 0;
            });
            for (var a in fieldOperatorLocationCache[i].assignedAlarms) {
                var alarm = fieldOperatorLocationCache[i].assignedAlarms[a];
                html += '<span>' + (Number(a)+1) + ': ' + alarm.patient.name + ' (' + alarm.occuranceAddress + ')<br />';
            }
            html += '</div>';

            // Add the assign buttons
            html += '<button class="btn btn-default assign-map-button';
            if (activeAlarm === null) html += ' disabled';
            html += '">@Messages.get("actions.button.map.assign")</button>';
            html += '<button class="btn btn-success pull-right dispatch-ring-btn">@Messages.get("button.ring")</button>';

            html += '</li>';
        }
        html += '</ul>';

        $('#map-sidebar-fieldoperators').html(html);

        // We bind the assign buttons if there is an active alarm, as well as render alarm details
        if (activeAlarm !== null) {
            bindAssignButtons();

            html = '<h4>Active alarm</h4>';
            html += '@Messages.get("patientpane.name"): <strong>' + activeAlarm.data.patient.name + '</strong><br />';
            html += '@Messages.get("patientpane.incident.location"): <strong>' + activeAlarm.data.occuranceAddress + '</strong><br />';
            html += '@Messages.get("patientpane.phonenumber"): <strong>' + activeAlarm.data.patient.phoneNumber + '</strong>';

            $('#map-sidebar-active-alarm').html(html).show();
        } else {
            $('#map-sidebar-active-alarm').hide();
        }
        bindCallButtons();
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
                // Shallow clone the array into cache
                fieldOperatorLocationCache = fieldOperatorLocations.slice(0);
                updateFieldOperatorMarkers();
                updateSidebar();
            });
        },

        getAlarmLocations: function (alarm) {
            $.getJSON('/alarm/allOpen', function (data) {
                if (DEBUG) console.log("Fetched all open alarms", data);
                alarms = [];
                for (var i = 0; i < data.total; i++) {
                    alarms.push(data.alarms[i]);
                }
                // Shallow clone the array into cache
                alarmCache = alarms.slice(0);

                // If we have an active alarm to select, replace alarms array with single item subset
                if (alarm !== null && alarm !== undefined) {
                    if (DEBUG) console.log("Set map view to active alarm mode: " + alarm.data.id + ' on Lat:' +
                        alarm.data.latitude + ' Lng:' + alarm.data.longitude);
                    alarms = [alarm.data];
                }
                updateAlarmMarkers();
            });
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
            fieldOperatorLocations = [];
            for (var i = 0; i < fieldOperatorLocationCache.length; i++) {
                var loc = fieldOperatorLocationCache[i];
                if (loc.id === fieldOperator.id) fieldOperatorLocations.push(loc);
            }
            if (DEBUG) console.log("FieldOp marker clicked, selected subset:", alarms);
            updateAlarmMarkers();
            updateFieldOperatorMarkers();

            // Pause the auto-refresh
            clearInterval(fieldOperatorLocationPeriodicUpdateTimer);
            fieldOperatorLocationPeriodicUpdateTimer = null;
        },

        // Resetting the view from subset to complete set of alarm markers
        resetSelectedAlarmsOnDisplay: function () {
            if (DEBUG) console.log('Resetting alarm display.');
            // If currently displayed alarms differ from that of the cache, we update current display set
            // and redraw the markers
            if (alarmCache.length !== alarms.length) {
                if (Alarms.getActiveAlarm() !== null) {
                    alarms = [Alarms.getActiveAlarm().data];
                } else {
                    // Shallow clone the contents of the cache back into the display array
                    alarms = alarmCache.slice(0);
                }

                updateAlarmMarkers();
            }
            // If currently displayed fieldoperatorlocations differ from that of the cache, we update current
            // display set and redraw the markers
            if (fieldOperatorLocationCache.length !== fieldOperatorLocations.length) {
                fieldOperatorLocations = fieldOperatorLocationCache.slice(0);
                updateFieldOperatorMarkers();
            }

            // Reset sidebar if needed
            if (highlightedOperator !== null) {
                highlightedOperator.removeClass('active');
                highlightedOperator.find('.assignmentQueue').hide();
            }

            // Re-enable periodic updates
            if (fieldOperatorLocationPeriodicUpdateTimer === null) {
                fieldOperatorLocationPeriodicUpdateTimer = setInterval(MapView.getAllCurrentPositions, UPDATE_INTERVAL);
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

var asyncMapViewLoader = function () {
    var script = document.createElement('script');
    script.type = 'text/javascript';
    script.src = 'https://maps.googleapis.com/maps/api/js?v=3&callback=MapView.init'
    document.body.appendChild(script);
}

window.onload = asyncMapViewLoader;