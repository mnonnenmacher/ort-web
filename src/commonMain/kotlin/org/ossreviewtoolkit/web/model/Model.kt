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

package org.ossreviewtoolkit.web.model

import kotlinx.serialization.Serializable

@Serializable
data class AnalyzerResult(
    val id: String,
    val timestamp: Long,
    val projects: Int,
    val packages: Int,
    val issues: Int
)

@Serializable
data class AnalyzerRun(
    val id: String,
    val timestamp: Long,
    val revision: String,
    val reference: String,
    val status: AnalyzerRunStatus,
    val projects: List<Project>
)

@Serializable
enum class AnalyzerRunStatus {
    QUEUED, ANALYZING_DEPENDENCIES, DOWNLOADING_SOURCE_CODE, RUNNING, FAILED, SUCCESS
}

@Serializable
data class CopyrightFinding(
    val statement: String,
    val location: TextLocation
)

@Serializable
data class Identifier(
    val type: String,
    val namespace: String,
    val name: String,
    val version: String
) {
    fun toCoordinates(): String = "$type:$namespace:$name:$version"
}

@Serializable
data class Issue(
    val timestamp: Long,
    val source: String,
    val severity: Severity,
    val message: String
)

@Serializable
data class LicenseFinding(
    val license: String,
    val location: TextLocation
)

@Serializable
data class Package(
    val id: String,
    val identifier: Identifier,
    val authors: Set<String>,
    val declaredLicenses: Set<String>,
    val concludedLicense: String,
    val detectedLicenses: Set<String>,
    val sourceArtifact: RemoteArtifact,
    val vcsInfo: VcsInfo,
    val lastScanStatus: ScannerRunStatus?,
    val lastScanId: String?
)

@Serializable
data class PackageReference(
    val id: Identifier,
    val dependencies: Set<PackageReference>,
    val issues: List<Issue>
)

@Serializable
data class Project(
    val id: Identifier,
    val definitionFilePath: String,
    val authors: Set<String>,
    val declaredLicenses: Set<String>,
    val vcs: VcsInfo,
    val homepageUrl: String,
    val scopes: List<Scope>
)

@Serializable
data class ProjectRepository(
    val id: String,
    val path: String,
    val repository: Repository
)

@Serializable
data class RemoteArtifact(
    val url: String,
    val hash: String,
    val hashAlgorithm: String
)

@Serializable
data class Repository(
    val id: String,
    val type: RepositoryType,
    val url: String
)

@Serializable
enum class RepositoryType {
    GIT,
    MERCURIAL,
    SUBVERSION,
    CVS;

    companion object {
        fun from(name: String): RepositoryType? = values().find { it.name.equals(name, ignoreCase = true) }
    }
}

@Serializable
enum class ScanJobStatus {
    QUEUED, IN_PROGRESS
}

@Serializable
data class ScannerRun(
    val id: String,
    val timestamp: Long,
    val provenance: String,
    val scanner: String,
    val status: ScannerRunStatus
)

@Serializable
enum class ScannerRunStatus {
    QUEUED, SCANNING, FAILED, SUCCESS
}

@Serializable
data class ScanSummary(
    val fileCount: Int,
    val packageVerificationCode: String,
    val licenseFindings: Set<LicenseFinding>,
    val copyrightFindings: Set<CopyrightFinding>,
    val issues: List<Issue> = emptyList()
)

@Serializable
data class Scope(
    val name: String,
    val dependencies: Set<PackageReference>
)

@Serializable
enum class Severity {
    ERROR, WARNING, HINT
}

@Serializable
data class TextLocation(
    val path: String,
    val startLine: Int,
    val endLine: Int
)

@Serializable
data class VcsInfo(
    val type: VcsType,
    val url: String,
    val revision: String,
    val resolvedRevision: String?,
    val path: String
)

@Serializable
enum class VcsType(val alias: String) {
    GIT("Git"),
    GIT_REPO("GitRepo"),
    MERCURIAL("Mercurial"),
    SUBVERSION("Subversion"),
    CVS("CVS"),
    UNKNOWN("Unknown")
}

@Serializable
data class WebProject(
    val id: String,
    val name: String
)
