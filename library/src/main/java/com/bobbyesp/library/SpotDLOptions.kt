package com.bobbyesp.library

/**
 * A helper class to manage command-line options for a SpotDLRequest.
 * It stores options and their arguments in a map and provides a method
 * to build them into a command-line-ready list of strings.
 */
class SpotDLOptions {
    // Using a LinkedHashMap to preserve the insertion order of options, which can be helpful for debugging.
    private val options: MutableMap<String, MutableList<String>> = LinkedHashMap()

    /**
     * Adds an option with a string argument.
     * If the option already exists, the new argument is appended to its list of arguments.
     * @param option The option flag (e.g., "--format").
     * @param argument The value for the option (e.g., "mp3").
     */
    fun addOption(option: String, argument: String): SpotDLOptions {
        val arguments = options.getOrPut(option) { mutableListOf() }
        arguments.add(argument)
        return this
    }

    /**
     * Adds an option with a numeric argument. The number is converted to a string.
     * @param option The option flag (e.g., "--threads").
     * @param argument The value for the option (e.g., 4).
     */
    fun addOption(option: String, argument: Number): SpotDLOptions {
        val arguments = options.getOrPut(option) { mutableListOf() }
        arguments.add(argument.toString())
        return this
    }

    /**
     * Adds a flag-only option (one without a value).
     * @param option The flag (e.g., "--no-cache").
     */
    fun addOption(option: String): SpotDLOptions {
        val arguments = options.getOrPut(option) { mutableListOf() }
        arguments.add("") // An empty string signifies a flag with no argument.
        return this
    }

    /**
     * Checks if a given option has been added.
     */
    fun hasOption(option: String): Boolean {
        return options.containsKey(option)
    }

    /**
     * Builds the map of options into a list of strings suitable for ProcessBuilder.
     * Example: {"--format": ["mp3"], "--no-cache": [""]} becomes ["--format", "mp3", "--no-cache"].
     * @return A list of strings representing the command-line options.
     */
    fun buildOptions(): List<String> {
        val commandList: MutableList<String> = mutableListOf()
        for ((option, arguments) in options) {
            for (argument in arguments) {
                commandList.add(option)
                if (argument.isNotEmpty()) {
                    commandList.add(argument)
                }
            }
        }
        return commandList
    }
}