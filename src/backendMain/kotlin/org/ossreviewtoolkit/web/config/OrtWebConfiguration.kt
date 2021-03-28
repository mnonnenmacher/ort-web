/*
 * Copyright (C) 2021 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package org.ossreviewtoolkit.web.config

import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.ConfigResult
import com.sksamuel.hoplite.Node
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.fp.getOrElse
import com.sksamuel.hoplite.fp.valid
import com.sksamuel.hoplite.parsers.toNode

import java.io.File

import org.ossreviewtoolkit.model.config.PostgresStorageConfiguration
import org.ossreviewtoolkit.utils.log

/**
 * The configuration model for ORT Web.
 */
data class OrtWebConfiguration(
    /**
     * Configuration of the PostgreSQL database connection.
     */
    val postgres: PostgresStorageConfiguration? = null
) {
    companion object {
        /**
         * Load the [OrtWebConfiguration]. The different sources are used with this priority:
         *
         * 1. [Command line arguments][args]
         * 2. [Configuration file][configFile]
         * 3. default.conf from resources
         *
         * The configuration file is optional and does not have to exist. However, if it exists, but does not
         * contain a valid configuration, an [IllegalArgumentException] is thrown.
         */
        fun load(args: Map<String, String> = emptyMap(), configFile: File): OrtWebConfiguration {
            if (configFile.isFile) {
                log.debug { "Using ORT configuration file at '$configFile'." }
            }

            val result = ConfigLoader.Builder()
                .addSource(argumentsSource(args))
                .addSource(PropertySource.file(configFile, optional = true))
                .addSource(PropertySource.resource("/default.conf"))
                .build()
                .loadConfig<OrtConfigurationWrapper>()

            return result.map { it.web }.getOrElse { failure ->
                if (configFile.isFile) {
                    throw IllegalArgumentException(
                        "Failed to load configuration from ${configFile.absolutePath}: ${failure.description()}"
                    )
                }

                if (args.keys.any { it.startsWith("web.") }) {
                    throw java.lang.IllegalArgumentException(
                        "Failed to load configuration from arguments $args: ${failure.description()}"
                    )
                }

                OrtWebConfiguration()
            }
        }

        /**
         * Generate a [PropertySource] providing access to the [args] the user has passed on the command line.
         */
        private fun argumentsSource(args: Map<String, String>): PropertySource {
            val node = args.toProperties().toNode("arguments").valid()
            return object : PropertySource {
                override fun node(): ConfigResult<Node> = node
            }
        }
    }
}

/**
 * An internal wrapper class to hold an [OrtWebConfiguration]. This class is needed to correctly map the _web_
 * prefix in configuration files when they are processed by the underlying configuration library.
 */
internal data class OrtConfigurationWrapper(
    val web: OrtWebConfiguration
)
