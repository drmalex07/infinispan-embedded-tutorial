package org.infinispan.tutorial.embedded;

import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStartedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;

@Listener
public class SimpleClusterListener
{
    public SimpleClusterListener() {}

    @CacheStarted
    public void viewChanged(CacheStartedEvent event)
    {
        System.out.printf("---- Cache started  ---- \n");
    }

    @ViewChanged
    public void viewChanged(ViewChangedEvent event)
    {
        System.out.printf("---- Cluster View changed: %s ----\n", event.getNewMembers());
    }
}
