package com.bobbyesp.library

/**
 * A data class to construct a spotdl command request.
 * It cleanly separates the main query (URLs or search terms)
 * from the various command-line options.
 */
class SpotDLRequest {
    // A list of URLs or search queries to be processed by spotdl.
    private val query: MutableList<String> = ArrayList()

    // A map of command-line options (e.g., --format mp3).
    private val options = SpotDLOptions()

    /**
     * Adds a URL or search term to the request. This is the primary subject
     * of the spotdl command.
     * @param url The Spotify URL or search term.
     */
    fun addUrl(url: String): SpotDLRequest {
        query.add(url)
        return this
    }

    /**
     * Adds a command-line option with a string argument.
     * Example: addOption("--format", "mp3")
     */
    fun addOption(option: String, argument: String): SpotDLRequest {
        options.addOption(option, argument)
        return this
    }
    
    /**
     * Overload for convenience when adding options with numeric arguments.
     * Example: addOption("--threads", 4)
     */
    fun addOption(option: String, argument: Number): SpotDLRequest {
        options.addOption(option, argument)
        return this
    }

    /**
     * Overload for flag-only options that don't have an argument.
     * Example: addOption("--no-cache")
     */
    fun addOption(option: String): SpotDLRequest {
        options.addOption(option)
        return this
    }

    /**
     * Checks if a specific option has been added to the request.
     */
    fun hasOption(option: String): Boolean {
        return options.hasOption(option)
    }

    /**
     * Assembles the final list of command arguments to be passed to the Python process.
     * The structure is always [<options>, <queries>], as expected by spotdl v4+.
     * @return A list of strings representing the full command arguments.
     */
    fun buildCommand(): List<String> {
        val commandList: MutableList<String> = ArrayList()
        commandList.addAll(options.buildOptions())
        commandList.addAll(query)
        return commandList
    }
}