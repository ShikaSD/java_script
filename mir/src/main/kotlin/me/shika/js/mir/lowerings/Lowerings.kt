package me.shika.js.mir.lowerings

val Lowerings = listOf(
    MirFileClassWrapLowering(),
    MirFunctionClassConvertLowering(),
    MirClinitLowering()
)

