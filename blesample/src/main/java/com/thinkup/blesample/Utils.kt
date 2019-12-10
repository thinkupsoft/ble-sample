package com.thinkup.blesample

object Utils {
    fun <T> getAttrs(clazz: Class<T>): List<String> {
        return clazz.fields.map { it.name }
    }

    fun <T> getValue(clazz: Class<T>, field: String): Any? {
        return clazz.fields.find { it.name == field }?.get(clazz)
    }
}