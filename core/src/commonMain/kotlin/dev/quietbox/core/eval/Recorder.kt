package dev.quietbox.core.eval

import dev.quietbox.core.cloud.CloudModel
import dev.quietbox.core.cloud.CloudReply
import dev.quietbox.core.cloud.CloudRequest

class Recorder(private val inner: CloudModel) : CloudModel {
    override val name = inner.name

    private val _seen = mutableListOf<CloudRequest>()
    val seen: List<CloudRequest> get() = _seen

    override suspend fun complete(request: CloudRequest): CloudReply {
        _seen += request
        return inner.complete(request)
    }
}
