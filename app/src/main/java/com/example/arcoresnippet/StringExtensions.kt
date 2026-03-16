package com.example.arcoresnippet

import android.net.Uri
import java.io.File

fun String.toFileUri(): Uri {
    val file = File(this)

    return Uri.fromFile(file)
}