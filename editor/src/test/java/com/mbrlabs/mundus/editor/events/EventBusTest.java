package com.mbrlabs.mundus.editor.events;

import com.mbrlabs.mundus.editorcommons.Subscribe;
import com.mbrlabs.mundus.editorcommons.events.SceneChangedEvent;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author JamesTKhan
 * @version June 27, 2023
 */
public class EventBusTest {

    @Test
    public void eventsReceivedTest() {
        EventBus eventBus = new EventBus();
        final int[] sceneChangedEventsReceived = {0};
        final int[] sceneAddedEventsReceived = {0};

        eventBus.register(new Object() {
            @Subscribe
            public void onEvent(SceneChangedEvent event) {
                sceneChangedEventsReceived[0]++;
            }
        });

        eventBus.register(new Object() {
            @Subscribe
            public void onEvent(SceneAddedEvent event) {
                sceneAddedEventsReceived[0]++;
            }
        });

        int sceneChangedCount = 10;
        int sceneAddedCount = 5;

        for (int i = 0; i < sceneChangedCount; i++) {
            eventBus.post(new SceneChangedEvent(null));
        }

        for (int i = 0; i < sceneAddedCount; i++) {
            eventBus.post(new SceneAddedEvent(null));
        }

        // check if events were received
        Assert.assertEquals(sceneChangedEventsReceived[0], sceneChangedCount);
        Assert.assertEquals(sceneAddedEventsReceived[0], sceneAddedCount);
    }

    @Test
    public void unregisterTest() {
        EventBus eventBus = new EventBus();
        final int[] sceneChangedEventsReceived = {0};

        Object subscriber = new Object() {
            @Subscribe
            public void onEvent(SceneChangedEvent event) {
                sceneChangedEventsReceived[0]++;
            }
        };

        // register and post, should receive 1 event
        eventBus.register(subscriber);
        eventBus.post(new SceneChangedEvent(null));

        // unregister and post, should not receive any events
        eventBus.unregister(subscriber);
        eventBus.post(new SceneChangedEvent(null));

        // should still be 1
        Assert.assertEquals(sceneChangedEventsReceived[0], 1);
    }

}