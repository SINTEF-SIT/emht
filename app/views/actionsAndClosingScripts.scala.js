@import play.i18n._

var Actions = (function ($) {
    /* Private methods here */

    /* Public methods inside return object */
    return {
        init: function () {
            $(".dispatch-ring-btn").click(function() { $('#calling_modal').modal("show") });
            $(".dispatch-send-btn").click(function() {
                //clear modal
                $("#dispatch_data_modal").find(':checkbox').each(
                    function() {
                        $(this).prop("checked", "checked");
                    }
                );

                // show modal
                $('#dispatch_data_modal').modal("show");
            });

            // setting up schedule time modal
            $(".schedule-btn").click(function() {
                //clear modal
                $("#schedule_time_modal").find(':checkbox').each(
                    function() {
                        $(this).prop("checked", "checked");
                    }
                );

                // show modal
                $('#schedule_time_modal').modal("show");
            });

            $("#schedule-time-picker").datetimepicker();
            $("#schedule_time_modal_btn").click(Actions.closeCaseAtClosing);
            // end of setting up schedule time modal

            $("#closeCaseActionsButton").click(Actions.closeCaseAtClosing);
            $("#saveAndFollowUpButton").click(Actions.saveAndFollowupAtClosing);

            // setting up organization tables so that the rows can be selected
            $("#kontaktpersonerTable > tbody").on("click","tr",Actions.organizationTableClick);
            $("#ambulerendeTable > tbody").on("click","tr",Actions.organizationTableClick);
            $("#legevaktTable > tbody").on("click","tr",Actions.organizationTableClick);
            $("#tpTable > tbody").on("click","tr",Actions.organizationTableClick);
            $("#hjemmesykepleienTable > tbody").on("click","tr",Actions.organizationTableClick);

            Actions.reset();
            // make sure that no phone is expanded
        },

        organizationTableClick: function () {
            $(this).closest('table').find('tr.active').removeClass('active');
            $(this).addClass('active');
        },

        reset: function () {
            $("#dynamicDispatchButtons").find(".in").removeClass("in");
        },

        //simple function that just gets the data from the  page and package it
        //into a json object
        getUpdatedAlarmFromPage: function () {
            var patientId = $('#dynamicPatientInfo').find('#patientId').val();
            var alarmId = $('#allListsSection').find('.list-group-item.active').attr("idnum");
            var notes = $('#globalNotesBox').val();

            var occuranceAddress = $('#incidentAddress').val();

            var updatedAlarm = {
                'alarmId' : alarmId,
                'notes' : notes,
                'occuranceAddress' : occuranceAddress,
                'patient' : {
                    'patientId' : patientId
                }
            };
            return updatedAlarm;
        },

        closeCaseAtClosing: function () {
            var updatedAlarm = Actions.getUpdatedAlarmFromPage();

            myJsRoutes.controllers.Application.closeCase().ajax({
                data : JSON.stringify(updatedAlarm),
                contentType : 'application/json',
                success : function (data) {
                    Alarms.gui.removeHighlightedAlarmFromList();
                    Alarms.gui.clearUpData();
                    //highlightBackListTab ();
                }// end of success
            });// end of ajax call
        },

        saveAndFollowupAtClosing: function () {
            var updatedAlarm = Actions.getUpdatedAlarmFromPage();

            myJsRoutes.controllers.Application.saveAndFollowupCase().ajax({
                data : JSON.stringify(updatedAlarm),
                contentType : 'application/json',
                success : function (data) {
                    Alarms.gui.moveAlarmToFollowUpList();
                    Alarms.gui.clearUpData();
                    //highlightBackListTab ();
                }// end of success
            });// end of ajax call
        },

        actionsDataSent: function () {
            $('#send_confirmation_modal').modal();
        }
    }
})(jQuery);
