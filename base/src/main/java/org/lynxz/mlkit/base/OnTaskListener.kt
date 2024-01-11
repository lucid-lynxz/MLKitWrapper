package org.lynxz.mlkit.base

interface OnTaskListener<TResult> {

    fun onSuccess(taskId: Int, result: TResult)


    fun onFailure(taskId: Int, e: Exception){

    }
}