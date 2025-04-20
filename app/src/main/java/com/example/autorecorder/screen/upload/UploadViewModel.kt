package com.example.autorecorder.screen.upload

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.autorecorder.database.PlanRepository
import com.example.autorecorder.database.TaskRepository
import com.example.autorecorder.entity.Plan
import com.example.autorecorder.entity.Task
import com.example.autorecorder.services.UploadService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

object UploadViewModel: ViewModel() {
    private val planRepository = PlanRepository()
    private val taskRepository = TaskRepository()
    private val _taskFlow = MutableStateFlow<List<Task>>(emptyList())
    private val taskFlow: StateFlow<List<Task>> = _taskFlow
        .onSubscription {
            _taskFlow.value.ifEmpty {
                kotlin.runCatching {
                    _taskFlow.value = taskRepository.getAllItems()
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList(),
        )
    private val _planFlow = MutableStateFlow<List<Plan>?>(null)
    val planFlow: StateFlow<List<Plan>?> = _planFlow
        .onSubscription {
            kotlin.runCatching {
                _planFlow.value = planRepository.getAllItems()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList(),
        )
    private val _runningFlow = MutableStateFlow<MutableMap<String, Pair<Job, Int>>>(mutableMapOf())
    val runningFlow: StateFlow<MutableMap<String, Pair<Job, Int>>> = _runningFlow.asStateFlow()

    val planList: StateFlow<List<Plan>> = combine(taskFlow, planFlow) { tasks, plans ->
        (plans ?: emptyList()).map { plan ->
            val planTasks = tasks.filter { it.planId == plan.id }
            val totalParts = planTasks.sumOf {
                it.chunksNum
            }
            val completedParts = planTasks.sumOf { it.partNumbers.size }
            val progress =
                if (totalParts > 0) (completedParts.toDouble() / totalParts) * 100 else 0.0
            plan.copy(progress = progress)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = emptyList(),
    )
    val runningList: StateFlow<List<Pair<Plan, Int>>> = combine(planList, runningFlow) { plans, running ->
        plans.filter { running.keys.contains(it.id) }.map {
            Pair(it, running[it.id]?.second ?: 0)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = emptyList(),
    )

    fun updateTasks(task: Task) {
        viewModelScope.launch {
            _taskFlow.value.firstOrNull {
                it.uploadId == task.uploadId
            }?.let {
                _taskFlow.value = _taskFlow.value.toMutableList().apply {
                    this[indexOf(it)] = task
                }
            } ?: run {
                _taskFlow.value = _taskFlow.value.toMutableList().apply {
                    add(task)
                }
            }
        }
    }

    fun updatePlans(plan: Plan) {
        viewModelScope.launch {
            (_planFlow.value ?: emptyList()).firstOrNull {
                it.id == plan.id
            }?.let {
                _planFlow.value = (_planFlow.value ?: emptyList()).toMutableList().apply {
                    this[indexOf(it)] = plan
                }
            } ?: run {
                _planFlow.value = (_planFlow.value ?: emptyList()).toMutableList().apply {
                    add(plan)
                }
            }
        }
    }

    fun cancelUpload(planId: String) {
        viewModelScope.launch {
            val map = _runningFlow.value
            map[planId]?.first?.cancel()
            map.remove(planId)
            _runningFlow.value = map
        }
    }

    fun deletePlan(plan: Plan) {
        viewModelScope.launch {
            runCatching {
                planRepository.deleteItem(plan)
                taskRepository.deleteItem(listOf(plan.id))
                _planFlow.value = planRepository.getAllItems()
            }
        }
    }

    fun removeUpload(planId: String) {
        viewModelScope.launch {
            val map = _runningFlow.value
            map.remove(planId)
            _runningFlow.value = map
        }
    }

    fun upsertUpload(planId: String, job: Job, notificationId: Int) {
        viewModelScope.launch {
            val map = _runningFlow.value
            map[planId]?.first?.cancel()
            map.remove(planId)
            map[planId] = Pair(job, notificationId)
            _runningFlow.value = map
        }
    }

    fun startUpload(context: Context, plan: Plan) {
        val intent = Intent(context, UploadService::class.java)
        intent.action = "START_UPLOAD"
        intent.putExtra("plan", plan)
        context.startService(intent)
    }

    fun cancelUpload(context: Context, planId: String) {
        val intent = Intent(context, UploadService::class.java)
        intent.action = "CANCEL_UPLOAD"
        intent.putExtra("planId", planId)
        context.startService(intent)
    }
}