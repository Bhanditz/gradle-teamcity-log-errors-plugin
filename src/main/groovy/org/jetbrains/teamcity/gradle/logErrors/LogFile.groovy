package org.jetbrains.teamcity.gradle.logErrors

import java.util.regex.Pattern
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage

class Message {
    String  filename
    Integer lineNumber
    String  status
    String  text = ''
    String  stacktrace = ''

    Message(filename) {
        this.filename = filename
    }

    String toString() {
        return this.properties
    }
}

class LogFile {
    File file
    Pattern pattern
    List<Message> errors = []

    LogFile(File file, Pattern pattern){
        this.file = file
        this.pattern = pattern
    }

    List<Message> parse() {
        if (!file.exists()) {
            System.err.println "File '$file.name' does not exist"
            return
        }

        Message message = new Message(file.name)

        file.eachLine { line, number ->
            def matcher = pattern.matcher(line)
            if (!matcher.matches()) {
                if (message.stacktrace == '') {
                    if (line ==~ /\S+Exception: .+/) {
                        // the line is a beginning of a stacktrace
                        message.stacktrace = line
                    } else {
                        // the line is not a stacktrace, sot it's a continuation of a log message
                        message.text += "\n$line"
                    }
                } else {
                    // the line is a continuation of a stacktrace
                    message.stacktrace += "\n$line"
                }
                return
            }

            // a line is a new log message. save previously collected message
            save(message)

            // Start a new message. In following lines it may be continued by multi-line text or a stacktrace
            message = new Message(file.name)
            message.lineNumber = number
            message.status = matcher[0][1].toLowerCase()
            message.text = matcher[0][2]
        }
        save(message)
        return errors
    }

    private void save(Message message) {
        if (message.status == 'error' || message.stacktrace)
            errors << message
    }

    static void printError(Message message) {
        def text
        if (message.status == 'error') {
            text = 'Error message'
        } else if (message.stacktrace) {
            text = 'Stacktrace without ERROR message'
        } else {
            throw new Exception('Not an error message')
        }
        text += " in $message.filename (line $message.lineNumber): $message.text"
        if (message.stacktrace) {
            text += "\n$message.stacktrace"
        }
        def attrs = [
            description: text.toString(),
            identity   : (message.stacktrace ?: message.text).hashCode().toString()
        ]
        println new ServiceMessage('buildProblem', attrs).asString()
    }
}
