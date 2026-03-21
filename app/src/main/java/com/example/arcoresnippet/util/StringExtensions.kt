package com.example.arcoresnippet.util

import android.net.Uri
import java.io.File

fun String.toFileUri(): Uri {
    val file = File(this)

    return Uri.fromFile(file)
}