package com.descalante.snapshotsgarsande

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import java.util.*

@IgnoreExtraProperties
data class Snapshot(@get:Exclude var id: String = "",
                    var title: String = "",
                    var photoUrl: String ="",
                    var datePost: String ="",
                    var email: String = "",
                    var likeList: Map<String, Boolean> = mutableMapOf())
