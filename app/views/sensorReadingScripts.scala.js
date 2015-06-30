/**
 * Created by myth on 6/30/15.
 */

@import play.i18n._

var Sensor = (function ($) {
    /* Private fields */
    var CHART_WIDTH = 450;
    var CHART_HEIGHT = 300;
    var DOM;
    var options = {
        title: 'Sensor Data',
        width: CHART_WIDTH,
        height: CHART_HEIGHT,
        interpolateNulls: true,
        chartArea: {
            top: 30,
            left: 40,
            width: '70%',
            height: '85%'
        },
        legend: 'right'
    };
    var chart;
    var updateTimer;
    var currentDataSet;

    /* Private methods */

    // Fetches patient sensor data by ID
    var getDataForPatient = function (patientId) {
        $.getJSON('/component/' + patientId, function (data) {
            generateDataTable(data);
            Sensor.draw();
        });
    };

    // Extracts and pre-processes the data
    var prepareData = function (data) {
        var wrapper = [];
        switch (data.type) {
            case "vitals":
                for (var i in data.readings) {
                    var time = new Date(data.readings[i].date);
                    // Since each reading is an individual data point in the backend, we need to add the rows
                    // with nulls. Only the column in question is populated in the switch statements.
                    // The interpolateNulls=true flag in the options object takes care of the rest.
                    var row = [
                        [time.getHours(), time.getMinutes(), time.getSeconds()],
                        null,
                        null,
                        null,
                        null
                    ];
                    switch (data.readings[i].readingType) {
                        case "battery":
                            row[1] = data.readings[i].value;
                            break;
                        case "heartRate":
                            row[2] = data.readings[i].value;
                            break;
                        case "systolicPressure":
                            row[3] = data.readings[i].value;
                            break;
                        case "diastolicPressure":
                            row[4] = data.readings[i].value;
                            break;
                    }
                    wrapper.push(row);
                }
                break;
        }
        return wrapper;
    };

    var generateDataTable = function (data) {
        var dataSet = new google.visualization.DataTable();
        dataSet.addColumn('timeofday', '@Messages.get("sensors.graph.x")');
        switch (data.type) {
            case "vitals":
                dataSet.addColumn('number', '@Messages.get("sensors.graph.battery")');
                dataSet.addColumn('number', '@Messages.get("sensors.graph.heartrate")');
                dataSet.addColumn('number', '@Messages.get("sensors.graph.systolic")');
                dataSet.addColumn('number', '@Messages.get("sensors.graph.diastolic")');

                var preparedData = prepareData(data);
                for (var row in preparedData) {
                    dataSet.addRow(preparedData[row]);
                }
        }
        currentDataSet = dataSet;
    };

    /* Public methods */
    return {
        init: function () {
            DOM = document.getElementById('sensorTab');
            chart = new google.visualization.LineChart(DOM);
            console.log("Sensor module initialized.");
        },

        draw: function () {
            console.log("Drawing Sensor Chart");
            chart.draw(currentDataSet, options);
        },

        startAutoUpdate: function (patientId) {
            if (updateTimer !== null && updateTimer !== undefined) clearInterval(updateTimer);

            getDataForPatient(patientId);
            updateTimer = setInterval(function () {
                getDataForPatient(patientId);
            }, 10000);
        },

        stopAutoUpdate: function () {
            if (updateTimer !== null && updateTimer !== undefined) {
                clearInterval(updateTimer);
                updateTimer = null;
            }
        }
    }
})(jQuery);
