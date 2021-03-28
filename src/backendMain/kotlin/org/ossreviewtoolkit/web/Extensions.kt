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

package org.ossreviewtoolkit.web

import java.util.UUID

import org.ossreviewtoolkit.model.CopyrightFinding as OrtCopyrightFinding
import org.ossreviewtoolkit.model.Identifier as OrtIdentifier
import org.ossreviewtoolkit.model.LicenseFinding as OrtLicenseFinding
import org.ossreviewtoolkit.model.OrtIssue
import org.ossreviewtoolkit.model.PackageReference as OrtPackageReference
import org.ossreviewtoolkit.model.Project as OrtProject
import org.ossreviewtoolkit.model.RemoteArtifact as OrtRemoteArtifact
import org.ossreviewtoolkit.model.ScanSummary as OrtScanSummary
import org.ossreviewtoolkit.model.Scope as OrtScope
import org.ossreviewtoolkit.model.Severity as OrtSeverity
import org.ossreviewtoolkit.model.TextLocation as OrtTextLocation
import org.ossreviewtoolkit.model.VcsInfo as OrtVcsInfo
import org.ossreviewtoolkit.model.VcsType as OrtVcsType
import org.ossreviewtoolkit.web.model.CopyrightFinding
import org.ossreviewtoolkit.web.model.Identifier
import org.ossreviewtoolkit.web.model.Issue
import org.ossreviewtoolkit.web.model.LicenseFinding
import org.ossreviewtoolkit.web.model.PackageReference
import org.ossreviewtoolkit.web.model.Project
import org.ossreviewtoolkit.web.model.RemoteArtifact
import org.ossreviewtoolkit.web.model.ScanSummary
import org.ossreviewtoolkit.web.model.Scope
import org.ossreviewtoolkit.web.model.Severity
import org.ossreviewtoolkit.web.model.TextLocation
import org.ossreviewtoolkit.web.model.VcsInfo
import org.ossreviewtoolkit.web.model.VcsType

fun String.toUUID(): UUID = UUID.fromString(this)

fun OrtCopyrightFinding.toWeb() = CopyrightFinding(
    statement = statement,
    location = location.toWeb()
)

fun OrtIdentifier.toWeb() = Identifier(type, namespace, name, version)

fun OrtIssue.toWeb() = Issue(
    timestamp = timestamp.epochSecond,
    source = source,
    severity = severity.toWeb(),
    message = message
)

fun OrtLicenseFinding.toWeb() = LicenseFinding(
    license = license.toString(),
    location = location.toWeb()
)

fun OrtPackageReference.toWeb(): PackageReference = PackageReference(
    id = id.toWeb(),
    dependencies = dependencies.map { it.toWeb() }.toSet(),
    issues = issues.map { it.toWeb() }
)

fun OrtProject.toWeb() = Project(
    id = id.toWeb(),
    definitionFilePath = definitionFilePath,
    authors = authors,
    declaredLicenses = declaredLicensesProcessed.allLicenses.toSortedSet(),
    vcs = vcsProcessed.toWeb(),
    homepageUrl = homepageUrl,
    scopes = scopes.map { it.toWeb() }
)

fun OrtRemoteArtifact.toWeb() = RemoteArtifact(
    url = url,
    hash = hash.value,
    hashAlgorithm = hash.algorithm.name
)

fun OrtScanSummary.toWeb() = ScanSummary(
    fileCount = fileCount,
    packageVerificationCode = packageVerificationCode,
    licenseFindings = licenseFindings.mapTo(mutableSetOf()) { it.toWeb() },
    copyrightFindings = copyrightFindings.mapTo(mutableSetOf()) { it.toWeb() },
    issues = issues.map { it.toWeb() }
)

fun OrtScope.toWeb() = Scope(
    name = name,
    dependencies = dependencies.mapTo(mutableSetOf()) { it.toWeb() }
)

fun OrtSeverity.toWeb() = when (this) {
    OrtSeverity.ERROR -> Severity.ERROR
    OrtSeverity.WARNING -> Severity.WARNING
    OrtSeverity.HINT -> Severity.HINT
}

fun OrtTextLocation.toWeb() = TextLocation(
    path = path,
    startLine = startLine,
    endLine = endLine
)

fun OrtVcsInfo.toWeb() = VcsInfo(
    type = type.toWeb(),
    url = url,
    revision = revision,
    resolvedRevision = resolvedRevision,
    path = path
)

fun OrtVcsType.toWeb() = when (this) {
    OrtVcsType.GIT -> VcsType.GIT
    OrtVcsType.GIT_REPO -> VcsType.GIT_REPO
    OrtVcsType.MERCURIAL -> VcsType.MERCURIAL
    OrtVcsType.SUBVERSION -> VcsType.SUBVERSION
    OrtVcsType.CVS -> VcsType.CVS
    OrtVcsType.UNKNOWN -> VcsType.UNKNOWN
    else -> VcsType.UNKNOWN
}
