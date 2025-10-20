package com.bobbyesp.library

class SpotDLRequest {
    // Made 'urls' a public var to be set directly from other classes
    var urls: List<String> = emptyList()
    private var options = SpotDLOptions()
    private var customCommandList: MutableList<String> = ArrayList()
    
    // Property to hold the spotdl operation (e.g., "download", "save")
    private var operation: String? = null

    // Method to set the operation for the request.
    fun setOperation(op: String): SpotDLRequest {
        this.operation = op
        return this
    }

    fun addOption(option: String, argument: String): SpotDLRequest {
        options.addOption(option, argument)
        return this
    }

    fun addOption(option: String, argument: Number): SpotDLRequest {
        options.addOption(option, argument)
        return this
    }

    fun addOption(option: String): SpotDLRequest {
        options.addOption(option)
        return this
    }

    fun addCommands(commands: List<String>): SpotDLRequest {
        customCommandList.addAll(commands)
        return this
    }

    fun getOption(option: String): String? {
        return options.getArgument(option)
    }

    fun getArguments(option: String): List<String?>? {
        return options.getArguments(option)
    }

    fun hasOption(option: String): Boolean {
        return options.hasOption(option)
    }

    fun buildCommand(): List<String> {
        val commandList: MutableList<String> = ArrayList()
        
        // --- FINAL FIX: Corrected the argument order for spotdl v4+ CLI ---
        // The correct order is: <operation> <query> [options]

        // 1. The operation (e.g., "download") must come first.
        operation?.let { commandList.add(it) }

        // 2. The query (URLs) must come IMMEDIATELY after the operation.
        commandList.addAll(urls)

        // 3. All other options come after the query.
        commandList.addAll(options.buildOptions())
        commandList.addAll(customCommandList)
        // --------------------------------------------------------------------

        return commandList
    }
}