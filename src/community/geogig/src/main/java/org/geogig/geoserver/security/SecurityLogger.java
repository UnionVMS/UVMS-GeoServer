/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.security;

import static java.lang.String.format;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.geogig.geoserver.config.LogStore;
import org.locationtech.geogig.api.AbstractGeoGigOp;
import org.locationtech.geogig.api.Context;
import org.locationtech.geogig.api.ObjectId;
import org.locationtech.geogig.api.Ref;
import org.locationtech.geogig.api.porcelain.CloneOp;
import org.locationtech.geogig.api.porcelain.FetchOp;
import org.locationtech.geogig.api.porcelain.PullOp;
import org.locationtech.geogig.api.porcelain.PullResult;
import org.locationtech.geogig.api.porcelain.PushOp;
import org.locationtech.geogig.api.porcelain.RemoteAddOp;
import org.locationtech.geogig.api.porcelain.RemoteRemoveOp;
import org.locationtech.geogig.api.porcelain.TransferSummary;
import org.locationtech.geogig.api.porcelain.TransferSummary.ChangedRef;
import org.locationtech.geogig.api.porcelain.TransferSummary.ChangedRef.ChangeTypes;
import org.locationtech.geogig.repository.Repository;
import org.springframework.beans.factory.InitializingBean;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public class SecurityLogger implements InitializingBean {

    private static final Map<Class<? extends AbstractGeoGigOp<?>>, MessageBuilder<?>> WATCHED_COMMANDS;
    static {
        Builder<Class<? extends AbstractGeoGigOp<?>>, MessageBuilder<?>> builder = ImmutableMap
                .builder();

        builder.put(RemoteAddOp.class, new RemoteAddMessageBuilder());
        builder.put(RemoteRemoveOp.class, new RemoteRemoveMessageBuilder());
        builder.put(PullOp.class, new PullOpMessageBuilder());
        builder.put(PushOp.class, new PushOpMessageBuilder());
        builder.put(FetchOp.class, new FetchOpMessageBuilder());
        builder.put(CloneOp.class, new CloneOpMessageBuilder());

        WATCHED_COMMANDS = builder.build();
    }

    private LogStore logStore;

    private static SecurityLogger INSTANCE;

    public SecurityLogger(LogStore logStore) {
        this.logStore = logStore;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        INSTANCE = this;
    }

    public static boolean interestedIn(Class<? extends AbstractGeoGigOp<?>> clazz) {
        return WATCHED_COMMANDS.containsKey(clazz);
    }

    public static void logPre(AbstractGeoGigOp<?> command) {
        if (INSTANCE == null) {
            return;// not yet initialized
        }
        INSTANCE.pre(command);
    }

    public static void logPost(AbstractGeoGigOp<?> command, @Nullable Object retVal,
            @Nullable RuntimeException exception) {

        if (INSTANCE == null) {
            return;// not yet initialized
        }
        if (exception == null) {
            INSTANCE.post(command, retVal);
        } else {
            INSTANCE.error(command, exception);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void error(AbstractGeoGigOp<?> command, RuntimeException exception) {
        MessageBuilder builder = builderFor(command);
        String repoUrl = repoUrl(command);
        logStore.error(repoUrl, builder.buildError(command, exception), exception);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void post(AbstractGeoGigOp<?> command, Object commandResult) {
        MessageBuilder builder = builderFor(command);
        String repoUrl = repoUrl(command);
        logStore.info(repoUrl, builder.buildPost(command, commandResult));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void pre(AbstractGeoGigOp<?> command) {
        MessageBuilder builder = builderFor(command);
        String repoUrl = repoUrl(command);
        logStore.debug(repoUrl, builder.buildPre(command));
    }

    private MessageBuilder<?> builderFor(AbstractGeoGigOp<?> command) {
        MessageBuilder<?> builder = WATCHED_COMMANDS.get(command.getClass());
        Preconditions.checkNotNull(builder);
        return builder;
    }

    @Nullable
    private String repoUrl(AbstractGeoGigOp<?> command) {
        Context context = command.context();
        if (context == null) {
            return null;
        }
        Repository repository = context.repository();
        if (repository == null) {
            return null;
        }
        URI location = repository.getLocation();
        if (location == null) {
            return null;
        }
        String uri;
        try {
            File f = new File(location);
            if (f.getName().endsWith(".geogig")) {
                f = f.getParentFile();
            }
            uri = f.getAbsolutePath();
        } catch (Exception e) {
            uri = location.toString();
        }
        return uri;
    }

    private static abstract class MessageBuilder<T extends AbstractGeoGigOp<?>> {

        CharSequence buildPost(T command, Object commandResult) {
            return format("%s success. Parameters: %s", friendlyName(), params(command));
        }

        CharSequence buildPre(T c) {
            return format("%s: Parameters: %s", friendlyName(), params(c));
        }

        CharSequence buildError(T command, RuntimeException exception) {
            return format("%s failed. Parameters: %s. Error message: %s", friendlyName(),
                    params(command), exception.getMessage());
        }

        abstract String friendlyName();

        abstract String params(T command);
    }

    private static class RemoteAddMessageBuilder extends MessageBuilder<RemoteAddOp> {
        @Override
        String friendlyName() {
            return "Remote add";
        }

        @Override
        String params(RemoteAddOp c) {
            return format("name='%s', url='%s'", c.getName(), c.getURL());
        }
    }

    private static class RemoteRemoveMessageBuilder extends MessageBuilder<RemoteRemoveOp> {
        @Override
        String friendlyName() {
            return "Remote remove";
        }

        @Override
        String params(RemoteRemoveOp c) {
            return format("name='%s'", c.getName());
        }
    }

    private static class PullOpMessageBuilder extends MessageBuilder<PullOp> {
        @Override
        String friendlyName() {
            return "Pull";
        }

        @Override
        CharSequence buildPost(PullOp command, Object commandResult) {
            PullResult pr = (PullResult) commandResult;
            TransferSummary fr = pr.getFetchResult();
            StringBuilder sb = formatFetchResult(fr);
            return format("%s success. Parameters: %s. Changes: %s", friendlyName(),
                    params(command), sb);
        }

        @Override
        String params(PullOp c) {
            return format("remote=%s, refSpecs=%s, depth=%s, author=%s, author email=%s",
                    c.getRemoteName(), c.getRefSpecs(), c.getDepth(), c.getAuthor(),
                    c.getAuthorEmail());
        }
    }

    private static class PushOpMessageBuilder extends MessageBuilder<PushOp> {
        @Override
        String friendlyName() {
            return "Push";
        }

        @Override
        String params(PushOp c) {
            return format("remote=%s, refSpecs=%s", c.getRemoteName(), c.getRefSpecs());
        }
    }

    private static class FetchOpMessageBuilder extends MessageBuilder<FetchOp> {
        @Override
        String friendlyName() {
            return "Fetch";
        }

        @Override
        CharSequence buildPost(FetchOp command, Object commandResult) {
            TransferSummary fr = (TransferSummary) commandResult;
            StringBuilder sb = formatFetchResult(fr);
            return format("%s success. Parameters: %s. Changes: %s", friendlyName(),
                    params(command), sb);
        }

        @Override
        String params(FetchOp c) {
            return format("remotes=%s, all=%s, full depth=%s, depth=%s, prune=%s",
                    c.getRemoteNames(), c.isAll(), c.isFullDepth(), c.getDepth(), c.isPrune());
        }
    }

    private static class CloneOpMessageBuilder extends MessageBuilder<CloneOp> {
        @Override
        String friendlyName() {
            return "Clone";
        }

        @Override
        String params(CloneOp c) {
            return format("url=%s, branch=%s, depth=%s", c.getRepositoryURL().orNull(), c
                    .getBranch().orNull(), c.getDepth().orNull());
        }
    }

    private static final StringBuilder formatFetchResult(TransferSummary fr) {
        Map<String, Collection<ChangedRef>> refs = fr.getChangedRefs();

        StringBuilder sb = new StringBuilder();
        if (refs.isEmpty()) {
            sb.append("already up to date");
        } else {
            for (Iterator<Entry<String, Collection<ChangedRef>>> it = refs.entrySet().iterator(); it
                    .hasNext();) {
                Entry<String, Collection<ChangedRef>> entry = it.next();
                String remoteUrl = entry.getKey();
                Collection<ChangedRef> changedRefs = entry.getValue();
                sb.append(" From ").append(remoteUrl).append(": [");
                print(changedRefs, sb);
                sb.append("]");
            }
        }
        return sb;
    }

    private static String toString(ObjectId objectId) {
        return objectId.toString().substring(0, 8);
    }

    private static void print(Collection<ChangedRef> changedRefs, StringBuilder sb) {
        for (Iterator<ChangedRef> it = changedRefs.iterator(); it.hasNext();) {
            ChangedRef ref = it.next();
            Ref oldRef = ref.getOldRef();
            Ref newRef = ref.getNewRef();
            if (ref.getType() == ChangeTypes.CHANGED_REF) {
                sb.append(oldRef.getName()).append(" ");
                sb.append(toString(oldRef.getObjectId()));
                sb.append(" -> ");
                sb.append(toString(newRef.getObjectId()));
            } else if (ref.getType() == ChangeTypes.ADDED_REF) {
                String reftype = (newRef.getName().startsWith(Ref.TAGS_PREFIX)) ? "tag" : "branch";
                sb.append("* [new ").append(reftype).append("] ").append(newRef.getName())
                        .append(" -> ").append(toString(newRef.getObjectId()));
            } else if (ref.getType() == ChangeTypes.REMOVED_REF) {
                sb.append("x [deleted] ").append(oldRef.getName());
            } else {
                sb.append("[deepened]" + newRef.getName());
            }
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
    }
}
