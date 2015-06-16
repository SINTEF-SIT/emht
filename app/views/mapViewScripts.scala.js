/**
 * Created by myth on 6/16/15.
 */
// We need the map object to be on global scope
var map;

// The main MapView module
var MapView = (function ($) {
    /* Private fields */
    var DEBUG = true;
    var FIRST_RUN = true;
    var UPDATE_INTERVAL = 15000;
    var fieldOperatorLocations = [];
    var markers = [];

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

            MapView.getAllCurrentPositions();
            setInterval(MapView.getAllCurrentPositions, UPDATE_INTERVAL);

            // Flag firstRun as false to prevent having to re-initialize the map every time
            FIRST_RUN = false;
        });
        $('#close-map-button').on('click', function (e) {
            e.preventDefault();
            $('#map-dashboard').hide();
            $('#main-dashboard').show();
        });
    }

    // Helper method that draws markers on the map
    var updateMarkers = function () {
        // Clear the markers cache
        for (var i = 0; i < markers.length; i++) {
            markers[i].setMap(null);
        }
        markers = [];
        // Add all field operators
        for (var i = 0; i < fieldOperatorLocations.length; i++) {
            var pos = new google.maps.LatLng(fieldOperatorLocations[i].latitude, fieldOperatorLocations[i].longitude);
            var marker = new google.maps.Marker({
                position: pos,
                map: map,
                title: fieldOperatorLocations[i].username
            });

            // Add the marker to cache
            markers.push(marker);
        }
    }

    // Helper method that updates the sidebar
    var updateSidebar = function () {
        var html = '<ul class="map-field-operator">';
        for (var i = 0; i < fieldOperatorLocations.length; i++) {
            html += '<li id="field-operator' + fieldOperatorLocations[i].id + '"><strong>' +
                fieldOperatorLocations[i].username + '</strong><br /><small>' +
                fieldOperatorLocations[i].timestamp + '</small></li>';
        }
        html += '</ul>'
        $('#map-sidebar-fieldoperators').html(html);
    }

    /* Public methods inside return object */
    return {
        init: function () {
            // Add click listeners to hide and show map buttons
            bindButtons();
        },
        getAllCurrentPositions: function () {
            $.getJSON('/location/current', function (data) {
                if (DEBUG) console.log("Fetched current locations.", data);
                fieldOperatorLocations = data.users;
                updateMarkers();
                updateSidebar();
            });
        }
    }
})(jQuery);

$(document).ready(function () {
    MapView.init();
})