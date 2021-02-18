package tv.orale.truetime;

import android.util.Log;

import com.instacart.library.truetime.CacheInterface;
import com.instacart.library.truetime.TrueTimeRx;

import java.util.Date;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * TrueTimePlugin
 */
public class TrueTimePlugin implements MethodCallHandler {
    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "true_time");
        channel.setMethodCallHandler(new TrueTimePlugin());
    }

    Disposable initSubscription;

    @Override
    public void onMethodCall(MethodCall call, final Result result) {
        switch (call.method) {
            case "init":
                if (!(call.arguments instanceof Map)) {
                    throw new IllegalArgumentException("Map argument expected");
                }
                //Clear old subscriber
                if (initSubscription != null && !initSubscription.isDisposed())
                {
                    initSubscription.dispose();
                }

                initSubscription = TrueTimeRx.build()
                        .withConnectionTimeout((int) call.argument("timeout"))
                        .withRetryCount((int) call.argument("retryCount"))
                        .withLoggingEnabled((Boolean) call.argument("logging"))
                        .initializeRx((String) call.argument("ntpServer"))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new BiConsumer<Date, Throwable>() {
                            @Override
                            public void accept(Date date, Throwable throwable) throws Exception {
                                if (TrueTimeRx.isInitialized()) {
                                    result.success(true);
                                    initSubscription.dispose();

                                } else if (throwable != null) {
                                    result.error("Error initializing TrueTime", throwable.getMessage(), false);
                                    initSubscription.dispose();
                                }
                            }
                        });
                break;
            case "now":
                result.success(TrueTimeRx.now().getTime());
                break;
            default:
                result.notImplemented();
        }
    }
}
