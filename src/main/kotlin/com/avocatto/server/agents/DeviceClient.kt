package com.avocatto.server.agents

import com.google.cloud.firestore.Firestore
import com.google.firebase.cloud.FirestoreClient
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable

data class Device(
    val id: String = "",
    val name: String = "",
    val location: String = "",
    val agent: String = "",
    val tags: List<String> = listOf()
)


fun getDevice(deviceId: String): Maybe<Device> {
    val firestore: Firestore = FirestoreClient.getFirestore()
    return Maybe.create { emitter ->
        val docRef = firestore.collection("fleet").document(deviceId)
        try {
            val snapshot = docRef.get().get()  // Blocking call
            if (snapshot.exists()) {
                val device = snapshot.toObject(Device::class.java)?.copy(id = snapshot.id)
                if (device != null) {
                    emitter.onSuccess(device)
                } else {
                    emitter.onError(Throwable("Error parsing device"))
                }
            } else {
                emitter.onComplete()  // No document found
            }
        } catch (e: Exception) {
            emitter.onError(e)
        }
    }
}

fun listenToDevice(deviceId: String): Observable<Device> {

    val firestore: Firestore = FirestoreClient.getFirestore()

    return Observable.create { emitter ->
        val docRef = firestore.collection("fleet").document(deviceId)

        val listenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                emitter.onError(error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val device = snapshot.toObject(Device::class.java)
                device?.let { emitter.onNext(it) }
            }
        }

        emitter.setCancellable {
            listenerRegistration.remove()
        }
    }

}