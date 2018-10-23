package vdung.android.kloudy.data

import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.processors.UnicastProcessor
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber

sealed class Result<T>(val value: T) {
    data class Pending<T>(val previousResult: T) : Result<T>(previousResult)
    data class Success<T>(val result: T) : Result<T>(result)
    data class Error<T>(val error: Throwable, val previousResult: T) : Result<T>(previousResult)
}

abstract class NetworkResourcePublisher<LocalType, NetworkArg, NetworkType> : Publisher<Result<LocalType>> {

    protected abstract fun localData(): Publisher<LocalType>
    protected abstract fun shouldFetch(arg: NetworkArg, previousResult: LocalType): Boolean
    protected abstract fun fetchFromNetwork(arg: NetworkArg): Publisher<NetworkType>
    protected abstract fun saveNetworkResult(networkData: NetworkType)

    private val fetchProcessor = UnicastProcessor.create<NetworkArg>()

    private val flow: Publisher<Result<LocalType>> by lazy {
        val fetchArgs = fetchProcessor.onBackpressureLatest().publish().autoConnect()
        return@lazy Flowable.fromPublisher(localData())
                .switchMap { result ->
                    fetchArgs
                            .filter { shouldFetch(it, result) }
                            .switchMap { arg ->
                                Single.fromPublisher(fetchFromNetwork(arg))
                                        .doOnSuccess(this::saveNetworkResult)
                                        .ignoreElement()
                                        .andThen(Flowable.empty<Result<LocalType>>())
                                        .startWith(Result.Pending(result))
                                        .onErrorReturn { Result.Error(it, result) }
                            }
                            .startWith(Result.Success(result))
                }
                .replay(1)
                .autoConnect()
    }

    fun fetch(arg: NetworkArg) {
        fetchProcessor.onNext(arg)
    }

    override fun subscribe(s: Subscriber<in Result<LocalType>>) {
        flow.subscribe(s)
    }
}

inline fun <reified LocalType, reified NetworkType> NetworkResourcePublisher<LocalType, Unit, NetworkType>.fetch() {
    fetch(Unit)
}
