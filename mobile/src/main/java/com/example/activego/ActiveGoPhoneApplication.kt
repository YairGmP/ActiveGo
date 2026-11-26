package com.example.activego

import android.app.Application

/**
 * Declared so that PhoneRepository and PhoneListenerService share one process and
 * one instance of the in-memory state. No initialisation is needed beyond that.
 */
class ActiveGoPhoneApplication : Application()
