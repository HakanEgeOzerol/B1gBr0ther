package com.b1gbr0ther

import java.time.LocalDateTime
import com.b1gbr0ther.TimingStatus
import com.b1gbr0ther.TaskCategory

enum class CreationMethod
{
    Voice,
    Gesture,
}

class Task constructor(
    private var taskName: String,
    private var startTime: LocalDateTime,
    private var endTime: LocalDateTime,
    private var creationMethod: CreationMethod,
    private var timingStatus: TimingStatus,
    private var category: TaskCategory = TaskCategory.getDefault(),
    private var isPreplanned: Boolean? = false,
    private var isCompleted: Boolean? = false,
    private var isBreak: Boolean? = false,
    )
{

    fun setName(name: String)
    {
        this.taskName = name 
    }

    fun getName(): String
    {
        return this.taskName
    }

    fun setStartTime(startTime: LocalDateTime)
    {
        this.startTime = startTime
    }

    fun getStartTime():LocalDateTime
    {
        return this.startTime
    }

    fun setEndTime(endTime: LocalDateTime)
    {
        this.endTime = endTime
    }

    fun getEndTime():LocalDateTime
    {
        return this.endTime
    }

    fun getTimingStatus(): TimingStatus {
        return this.timingStatus
    }

    fun setTimingStatus(status: TimingStatus) {
        this.timingStatus = status
    }

    fun setCreationMethod(creationMethod: CreationMethod)
    {
        this.creationMethod = creationMethod
    }

    fun getCreationMethod(): CreationMethod
    {
        return this.creationMethod
    }

    fun setIsPreplanned(isPreplanned: Boolean)
    {
        this.isPreplanned = isPreplanned
    }

    fun getIsPreplanned(): Boolean?
    {
        return this.isPreplanned
    }

    fun setIsCompleted(isCompleted: Boolean)
    {
        this.isCompleted = isCompleted
    }

    fun getIsCompleted(): Boolean?
    {
        return this.isCompleted
    }

    fun setIsBreak(isBreak: Boolean)
    {
        this.isBreak = isBreak
    }

    fun getIsBreak(): Boolean?
    {
        return this.isBreak
    }

    fun setCategory(category: TaskCategory)
    {
        this.category = category
    }
    
    fun getCategory(): TaskCategory
    {
        return this.category
    }

    fun getTimeDif()
    {
//        TO DO
    }
}