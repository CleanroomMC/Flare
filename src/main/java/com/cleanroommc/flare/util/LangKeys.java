package com.cleanroommc.flare.util;

public enum LangKeys {

    // Modules
    MONITORING_START_PERCENTAGE_CHANGE("flare.message.monitoring_start.percentage_change", "Starting now, any ticks with >%f %% increase in duration compared to the average will be reported."),
    MONITORING_START_GREATER_DURATION("flare.message.monitoring_start.greater_duration", "Starting now, any ticks with duration >%f will be reported."),
    TICK_MONITORING_START("flare.message.tick_monitoring_start", "Tick monitor started. Before the monitor becomes fully active, the server's average tick rate will be calculated over a period of 120 ticks (approx 6 seconds)."),
    TICK_MONITORING_END("flare.message.tick_monitoring_end", "Analysis is now complete.\n> Max: %sms\n> Min: %sms\n> Average: %sms"),
    TICK_MONITORING_REPORT("flare.message.tick_monitoring_report", "Tick #%d lasted %s ms. (%s%% increased from average)"),
    TICK_MONITORING_GC_REPORT("flare.message.tick_monitoring_gc_report", "Tick #%d included GC lasting %s ms. (Type = %s)"),
    TPS_STATISTICS_RECALL("flare.message.tps_statistics_recall", "TPS from the last 5s, 10s, 1m, 5m, 15m:\n%s, %s, %s, %s, %s\n"),
    TPS_STATISTICS_DURATION_AVERAGES("flare.message.tps_statistics_duration_averages", "Tick durations (min/med/95%%ile/max ms) from the last 10s, 1m:\n%s ; %s\n"),
    CPU_USAGE_SYSTEM_LOAD("flare.message.cpu_usage_system_load", "CPU usage from the last 10s, 1m, 15m:\n%s, %s, %s  (system)\n"),
    CPU_USAGE_PROCESS_LOAD("flare.message.cpu_usage_process_load", "CPU usage from the last 10s, 1m, 15m:\n%s, %s, %s  (process)\n"),
    PING_STATISTICS_SPECIFIC_PLAYER("flare.message.ping_statistics_specific_player", "Player %s has %sms ping."),
    PING_STATISTICS_AVERAGES("flare.message.ping_statistics_averages", "Average pings (min/med/95%ile/max ms) from now and last 15m:\n%s, %s, %s, %s | %s, %s, %s, %s"),
    BASIC_MEMORY_USAGE_REPORT("flare.message.basic_memory_usage_report", "Memory Usage:\n    %s / %s   (%s)\n    %s"),
    DETAILED_MEMORY_USAGE_REPORT("flare.message.detailed_memory_usage_report", "Non-Heap Memory Usage:\n    %s\n"),
    MEMORY_POOL_REPORT("flare.message.memory_pool_report", "%s Pool Usage:\n    %s / %s    (%s)    %s"),
    MEMORY_POOL_COLLECTION_USAGE_REPORT("flare.message.memory_pool_collection_usage_report", "     - Usage at least GC %s"),
    NETWORKING_REPORT_HEADER("flare.message.networking_report_header", "Network Usage (system, last 15m):"),
    NETWORKING_REPORT_BODY("flare.message.networking_report_body", "\n    %s / %s pps (%s %s)"),
    DISK_USAGE_REPORT("flare.message.disk_usage_report", "Disk Usage:\n    %s / %s    (%s)\n    %s"),
    HEAP_SUMMARY_WAIT("flare.message.heap_summary_wait", "Creating a new Heap Summary, please wait..."),
    HEAP_SUMMARY_REPORT("flare.message.heap_summary_report", "Heap Summary Report:\n%s"),
    HEAP_SUMMARY_REPORT_USAGE_HINT("flare.message.heap_summary_report_usage_hint", "You can read the heap summary file using the viewer web-app - %s"),
    HEAP_DUMP_WAIT("flare.message.heap_dump_wait", "Creating a new Heap Dump, please wait..."),
    HEAP_DUMP_REPORT("flare.message.heap_dump_report", "Heap Dump Report:\n%s"),

    // Sampler
    SAMPLER_START("flare.message.sampler_start", "Sampler started!"),
    SAMPLER_INFO_START("flare.message.sampler_info_start", "No active sampler found, to start one run: /flare sampler start."),
    SAMPLER_INFO_STARTED("flare.message.sampler_info_started", "Active sampler found. Sampler has been running for %s."),
    SAMPLER_INFO_VIEW("flare.message.sampler_info_view", "To view the sampler while it is running, run: /flare sampler view."),
    SAMPLER_INFO_STOP("flare.message.sampler_info_stop", "Since the sampler is running indefinitely, to stop the sampler, run: /flare sampler stop."),
    SAMPLER_INFO_STOPPING("flare.message.sampler_info_stopping", "The sampler is due to stop in %s."),
    SAMPLER_INFO_CANCEL("flare.message.sampler_info_cancel", "To cancel the sampler without the output of results, run: /flare sampler cancel."),
    SAMPLER_CANCELLING("flare.message.sampler_cancelling", "Cancelling Sampler!"),
    SAMPLER_STOPPING("flare.message.sampler_stopping", "Stopping Sampler..."),
    SAMPLER_SAVED_REPORT("flare.message.sampler_saved_report", "Sampler Report Saved: %s"),
    SAMPLER_UPLOADED_REPORT("flare.message.sampler_uploaded_report", "Sampler Report Uploaded: %s"),
    SAMPLER_VIEWER_OPEN("flare.message.sampler_viewer_open", "Sampler's Live Viewer: %s"),
    SAMPLER_VIEWER_TRUST("flare.message.sampler_viewer_trust", "Client connected to the live viewer using id [%s] is now trusted!"),

    // General
    COMPRESS_FILE_START("flare.message.compress_file_start", "Compressing %s, please wait..."),
    COMPRESS_FILE_PROGRESS("flare.message.compress_file_progress", "Compressed %s / %s so far... (%s%%)"),
    COMPRESS_FILE_REPORT("flare.message.compress_file_report", "Compression complete: %s --> %s (%s%%)\nCompressed heap dump written to: %s"),

    // Module Exceptions
    PING_STATISTICS_SINGLEPLAYER("flare.message.error.ping_statistics_singleplayer", "Ping statistics isn't applicable for Singleplayer!"),
    PING_STATISTICS_NOT_ENOUGH_DATA("flare.message.error.ping_statistics_not_enough_data", "There isn't enough data to show ping averages yet. Please try again later."),
    INSPECTING_HEAP_UNEXPECTED_EXCEPTION("flare.message.error.inspecting_heap_unexpected_exception", "An error occurred whilst inspecting the heap."),

    // Sampler Exceptions
    SAMPLER_ALREADY_STARTED("flare.message.sampler_already_started", "Sampler is already active."),
    SAMPLER_HAS_NOT_STARTED("flare.message.sampler_has_not_started", "Sampler has not started yet."),
    SAMPLER_START_TIMEOUT_TOO_SHORT("flare.message.sampler_start_timeout_too_short", "Specified timeout is too short and would lead to inaccurate results. Please specify more than 10 seconds."),
    SAMPLER_START_TIMEOUT_SHORT("flare.message.sampler_start_timeout_short", "Specified timeout may lead to inaccurate results, 30 seconds or more will significantly improve accuracy of sampling results."),
    SAMPLER_CANNOT_UPLOAD_REPORT("flare.message.sampler_cannot_upload_report", "Cannot upload sampler report! Error: %s (see console for more information). Attempting to save to disk instead."),
    SAMPLER_FAILED_UNEXPECTEDLY("flare.message.sampler_failed_unexpectedly", "Sampling operation failed unexpectedly! Error: %s (see console for more information)"),
    SAMPLER_VIEWER_UNSUPPORTED("flare.message.sampler_viewer_unsupported", "The live viewer is not supported in the current environment!"),
    SAMPLER_VIEWER_FAILED_UNEXPECTEDLY("flare.message.sampler_viewer_failed_unexpectedly", "The live viewer failed unexpectedly Error: %s (see console for more information)"),
    SAMPLER_VIEWER_TRUST_ID_NOT_FOUND("flare.message.sampler_viewer_trust_id_not_found", "Unable to find pending client with the id [%s]"),
    SAMPLER_VIEWER_ID_NOT_PROVIDED("flare.message.sampler_viewer_id_not_provided", "Please provide client id(s) with '--id <id>,<id2>...'"),

    // General Exceptions
    ERROR("flare.message.error", "Errored LangKey Entry. Report on GitHub!"),
    MALFORMED_FLAGS("flare.message.error.malformed_flags", "Malformed flags used in conjunction with this command, these are the available flags: [%s]"),
    MALFORMED_INPUTS("flare.message.error.malformed_inputs", "Malformed inputs used in conjunction with the flag: [%s], please refer to the usages."),
    NO_INPUTS("flare.message.error.no_inputs", "No inputs used in conjunction with the flag: [%s], please refer to the usages."),
    CANNOT_UPLOAD_SAVE_TO_DISK_INSTEAD("flare.message.error.cannot_upload_save_to_disk_instead", "An error occurred whilst uploading the data. Attempting to save to disk instead."),
    CANNOT_SAVE_TO_DISK("flare.message.error.cannot_save_to_disk", "An error occurred whilst saving the data."),
    CANNOT_COMPRESS_FILE("flare.message.error.cannot_compress_file", "An error occurred whilst compressing the file.")

    ;

    final String langKey, defaultText;

    LangKeys(String langKey, String defaultText) {
        this.langKey = langKey;
        this.defaultText = defaultText;
    }

}
