package com.wintersky.windyreader.data.source.remote;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.wintersky.windyreader.data.Book;
import com.wintersky.windyreader.data.Chapter;
import com.wintersky.windyreader.data.source.DataSource;
import com.wintersky.windyreader.util.AppExecutors;

import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaState;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.realm.RealmList;
import lombok.Cleanup;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.wintersky.windyreader.util.FileUtil.is2String;
import static com.wintersky.windyreader.util.LuaUtil.getLua;
import static com.wintersky.windyreader.util.LuaUtil.luaSafeDoString;
import static com.wintersky.windyreader.util.LuaUtil.luaSafeRun;

@Singleton
public class RemoteDataSource implements DataSource {

    private final Context mContext;
    private final AppExecutors mExecutors;
    private final OkHttpClient mHttp;

    private Future taskCatalog;
    private Future mContentFuture;

    @Inject
    RemoteDataSource(@NonNull Context context, @NonNull AppExecutors executors, @NonNull OkHttpClient http) {
        mContext = context;
        mExecutors = executors;
        mHttp = http;
    }

    @Deprecated
    @Override
    public void getShelf(@NonNull GetShelfCallback callback) {
        throw new NoSuchMethodError();
    }

    @Override
    public void getBook(@NonNull final String bkUrl, @NonNull final GetBookCallback callback) {
        mExecutors.networkIO().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final Book book = getBookFrom(bkUrl);
                    mExecutors.mainThread().execute(new Runnable() {
                        @Override
                        public void run() {
                            callback.onLoaded(book);
                        }
                    });
                } catch (final Exception e) {
                    mExecutors.mainThread().execute(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFailed(e);
                        }
                    });
                }
            }
        });
    }

    @Override
    public void getCatalog(@NonNull final Book book, @NonNull final GetCatalogCallback callback) {
        if (taskCatalog != null) {
            taskCatalog.cancel(true);
        }
        final String ctUrl = book.getCatalogUrl();
        taskCatalog = mExecutors.networkIO().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    final RealmList<Chapter> list = getCatalogFrom(ctUrl);
                    mExecutors.mainThread().execute(new Runnable() {
                        @Override
                        public void run() {
                            callback.onLoaded(list);
                            taskCatalog = null;
                        }
                    });
                } catch (final Exception e) {
                    mExecutors.mainThread().execute(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFailed(e);
                            taskCatalog = null;
                        }
                    });
                }
            }
        });
    }

    @Override
    public void getContent(@NonNull final Chapter chapter, @NonNull final GetContentCallback callback) {
        if (mContentFuture != null) {
            mContentFuture.cancel(true);
        }
        final String chUrl = chapter.getUrl();
        mContentFuture = mExecutors.networkIO().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    final String content = getContentFrom(chUrl);
                    mExecutors.mainThread().execute(new Runnable() {
                        @Override
                        public void run() {
                            callback.onLoaded(content);
                            mContentFuture = null;
                        }
                    });
                } catch (final Exception e) {
                    mExecutors.mainThread().execute(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFailed(e);
                            mContentFuture = null;
                        }
                    });
                }
            }
        });
    }

    @Deprecated
    @Override
    public void saveBook(@NonNull Book book, @NonNull SaveBookCallback callback) {
        throw new NoSuchMethodError();
    }

    @Deprecated
    @Override
    public void deleteBook(@NonNull String bkUrl, @NonNull DeleteBookCallback callback) {
        throw new NoSuchMethodError();
    }

    @NonNull
    public Book getBookFrom(@NonNull String url) throws LuaException, IOException {
        Request request = new Request.Builder().url(url).build();
        Response response = mHttp.newCall(request).execute();

        String doc;
        @Cleanup LuaState lua = getLua(mContext);
        String fileName = response.request().url().host().replace('.', '_') + ".lua";
        luaSafeDoString(lua, is2String(mContext.getAssets().open(fileName)), 1);
        lua.getField(-1, "charset");
        String charset = lua.toString(-1);
        lua.pop(1);

        @Cleanup ResponseBody body = response.body();
        if (body == null) {
            throw new IOException("ResponseBody null");
        }
        if (charset == null) {
            doc = body.string();
        } else {
            doc = body.source().readString(Charset.forName(charset));
        }

        lua.getField(-1, "getBook");
        lua.pushString(response.request().url().toString());
        lua.pushString(doc);
        luaSafeRun(lua, 2, 1);
        String json = lua.toString(-1);
        try {
            return new Gson().fromJson(json, Book.class);
        } catch (JsonSyntaxException e) {
            throw formatJsonError(url, json, e);
        }
    }

    @NonNull
    public RealmList<Chapter> getCatalogFrom(@NonNull String url) throws LuaException, IOException {
        Request request = new Request.Builder().url(url).build();
        Response response = mHttp.newCall(request).execute();

        String doc;
        @Cleanup LuaState lua = getLua(mContext);
        String fileName = response.request().url().host().replace('.', '_') + ".lua";
        luaSafeDoString(lua, is2String(mContext.getAssets().open(fileName)), 1);
        lua.getField(-1, "charset");
        String charset = lua.toString(-1);
        lua.pop(1);

        @Cleanup ResponseBody body = response.body();
        if (body == null) {
            throw new IOException("ResponseBody null");
        }
        if (charset == null) {
            doc = body.string();
        } else {
            doc = body.source().readString(Charset.forName(charset));
        }

        lua.getField(-1, "getCatalog");
        lua.pushString(url);
        lua.pushString(doc);
        luaSafeRun(lua, 2, 1);
        String json = lua.toString(-1);
        try {
            return new Gson().fromJson(json, new TypeToken<RealmList<Chapter>>() {}.getType());
        } catch (JsonSyntaxException e) {
            throw formatJsonError(url, json, e);
        }
    }

    @NonNull
    public String getContentFrom(@NonNull String url) throws LuaException, IOException {
        Request request = new Request.Builder().url(url).build();
        Response response = mHttp.newCall(request).execute();

        String doc;
        @Cleanup LuaState lua = getLua(mContext);
        String fileName = response.request().url().host().replace('.', '_') + ".lua";
        luaSafeDoString(lua, is2String(mContext.getAssets().open(fileName)), 1);
        lua.getField(-1, "charset");
        String charset = lua.toString(-1);
        lua.pop(1);

        @Cleanup ResponseBody body = response.body();
        if (body == null) {
            throw new IOException("ResponseBody null");
        }
        if (charset == null) {
            doc = body.string();
        } else {
            doc = body.source().readString(Charset.forName(charset));
        }

        lua.getField(-1, "getContent");
        lua.pushString(doc);
        luaSafeRun(lua, 1, 1);
        return lua.toString(-1);
    }

    @NonNull
    private JsonSyntaxException formatJsonError(@NonNull String url, @NonNull String json, @NonNull JsonSyntaxException e) {
        Matcher matcher = Pattern.compile("at line (\\d+) column (\\d+) path \\$\\.(\\w+)").matcher(e.getMessage());
        if (matcher.find()) {
            int line = Integer.valueOf(matcher.group(1));
            String error = json.split("\n")[line - 1];
            JsonSyntaxException exception = new JsonSyntaxException(e.getMessage());
            StackTraceElement[] elements = e.getStackTrace();
            StackTraceElement element = elements[0];
            element = new StackTraceElement(
                    String.format("%s\nurl: %s\nat %s", error, url, element.getClassName()),
                    element.getMethodName(), element.getFileName(), element.getLineNumber());
            elements[0] = element;
            exception.setStackTrace(elements);
            return exception;
        }
        return e;
    }
}
