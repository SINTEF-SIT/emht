/**
 * Created by myth on 6/16/15.
 */
// We need the map object to be on global scope
var map;

// The main MapView module
var MapView = (function ($) {
    /* Private fields */
    var firstRun = true;
    var locations = {};
    var markers = [];

    /* Private methods here */
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
            if (firstRun) {
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
            // Flag firstRun as false to prevent having to re-initialize the map every time
            firstRun = false;
        });
        $('#close-map-button').on('click', function (e) {
            e.preventDefault();
            $('#map-dashboard').hide();
            $('#main-dashboard').show();
        });
    }

    var updateMarkers = function () {
        
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
                locations = data;
            });
        }
    }
})(jQuery);

$(document).ready(function () {
    MapView.init();
})