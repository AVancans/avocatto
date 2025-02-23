package com.avocatto.server.agents

import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.subjects.BehaviorSubject

class AgentsService {

    val disposable = CompositeDisposable()
    val agents: BehaviorSubject<List<Agent>> = BehaviorSubject.createDefault(listOf())

    fun listen() {
        listenToAgents().subscribe { agentList ->
            println("Received list of agents (${agentList.size})")
            agents.onNext(agentList)
        }.also { disposable.add(it) }
    }

    fun getById(agentId: String): Agent? = agents.value?.firstOrNull { it.id == agentId}

    fun stop() {
        disposable.clear()
    }

}