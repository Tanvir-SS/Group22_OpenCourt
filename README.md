# OpenCourt

An Android application that tracks real-time availability of tennis and basketball courts near you. Users can check in/out of nearby courts, view live court status, filter courts by preferences, and save favourites for quick access. The app integrates an online real-time database, authentication, interactive maps, and crowdsourced court updates.

## Features

### Core Functionality

* **Real-time Court Availability**
  View live check-ins and check-outs from other users.

* **Proximity-Based Check-In (≤ 200m)**
  Users must be physically close to a court to check in/out.

* **Interactive Map View**

  * Markers for tennis and basketball courts
  * Search by location
  * Click markers for detailed court info
  * Favourite courts highlighted
  * Option to save map view state

* **User Authentication**

  * Email/password login
  * Google Sign-in support
  * Cloud-stored favourite courts

* **Court Database Management**

  * Add new court entries
  * Edit amenities, photos, and metadata
  * Weather integration for each court
  * Filter by sport, distance, and favourites

## Tech Stack

* **Platform:** Android (Kotlin)
* **Database:** Firebase Realtime Database
* **Auth:** Firebase Authentication (Email + Google)
* **Maps:** Google Maps SDK
* **Hosting (APK Download Page):** Vercel

## Installation

Download the latest APK from our website:

**[https://opencourt-group22.vercel.app/](https://opencourt-group22.vercel.app/)**

And install it on any android device or emulator.

## How It Works

1. Launch the app and sign in. Google Sign-in is supported.
2. Allow location permissions so the app can determine proximity to courts.
3. Browse the map to find tennis and basketball courts near you.
4. Tap a marker or a home page entry to view:

   * Current availability
   * Amenities
   * Weather
   * Photos
   * Favourites toggle
5. Check in/out when physically near a court.
6. Add or edit court details to improve data quality for all users.

## Demo Video

[Final Presentation + Demo](https://www.youtube.com/watch?v=zAXvRrGxMB4)

## Team Members

- Hugo Najafi
- Tanvir Shergill
- Rohin Aulakh
- Harry Gupta
  
