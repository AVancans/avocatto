package com.avocatto.server.agents

import com.google.cloud.firestore.Firestore
import com.google.firebase.cloud.FirestoreClient
import io.reactivex.rxjava3.core.Observable


data class Agent(
    val id: String = "",
    val system_prompt: String? = null,
    val first_message: String? = null,
    val voice: String = "",
)

fun listenToAgents(): Observable<List<Agent>> {

    val firestore: Firestore = FirestoreClient.getFirestore()
    return Observable.create { emitter ->
        val collectionRef = firestore.collection("agents")

        val listenerRegistration = collectionRef.addSnapshotListener { snapshots, error ->
            if (error != null) {
                emitter.onError(error)
                return@addSnapshotListener
            }

            if (snapshots != null && !snapshots.isEmpty) {
                val agentList = snapshots.documents.mapNotNull { doc ->
                    doc.toObject(Agent::class.java)?.copy(id = doc.id)
                }
                emitter.onNext(agentList) // Emit to Observable
            }
        }

        emitter.setCancellable {
            listenerRegistration.remove()
        }
    }

}