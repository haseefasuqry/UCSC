package com.ucsc.codescribe.navigation

object Routes {
    const val HOME = "home"
    const val EDITOR = "editor/{fileId}"
    const val VERSIONS = "versions/{fileId}"
    const val DIFF = "diff/{fileId}/{oldVersion}/{newVersion}"

    fun editor(fileId: Long) = "editor/$fileId"
    fun versions(fileId: Long) = "versions/$fileId"
    fun diff(fileId: Long, oldVersion: Int, newVersion: Int) = "diff/$fileId/$oldVersion/$newVersion"
}
