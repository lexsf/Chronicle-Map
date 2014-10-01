/*
 * Copyright 2014 Higher Frequency Trading
 * <p/>
 * http://www.higherfrequencytrading.com
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.collections;


import net.openhft.chronicle.map.ChronicleMapBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;

/**
 * Test VanillaSharedReplicatedHashMap where the Replicated is over a TCP Socket, but with 4 nodes
 *
 * @author Rob Austin.
 */
public class PostClusterMapCreationTest {

    private SharedHashMap<Integer, CharSequence> map1a;
    private SharedHashMap<Integer, CharSequence> map2a;

    private SharedHashMap<Integer, CharSequence> map1b;
    private SharedHashMap<Integer, CharSequence> map2b;

    private ClusterReplicator clusterB;
    private ClusterReplicator clusterA;

    private ClusterReplicatorBuilder clusterReplicatorBuilder;
    private ClusterReplicatorBuilder clusterReplicatorBuilder1;


    public static File getPersistenceFile() {
        String TMP = System.getProperty("java.io.tmpdir");
        File file = new File(TMP + "/test" + System.nanoTime());
        file.deleteOnExit();
        return file;
    }


    @Before

    public void setup() throws IOException {

        {
            final TcpReplicatorBuilder tcpReplicatorBuilder =
                    new TcpReplicatorBuilder(8086, new InetSocketAddress("localhost", 8087)).heartBeatInterval(1, SECONDS);

            clusterReplicatorBuilder = new ClusterReplicatorBuilder((byte) 1, 1024);
            clusterReplicatorBuilder.tcpReplicatorBuilder(tcpReplicatorBuilder);
            clusterA = clusterReplicatorBuilder.create();

            // this is how you add maps after the custer is created
            map1a = clusterReplicatorBuilder.create((short) 1,
                    ChronicleMapBuilder.of(Integer.class, CharSequence.class)
                            .entries(1000)
                            .file(getPersistenceFile()));

            map2a = clusterReplicatorBuilder.create((short) 2,
                    ChronicleMapBuilder.of(Integer.class, CharSequence.class)
                            .entries(1000)
                            .file(getPersistenceFile()));
        }


        {
            final TcpReplicatorBuilder tcpReplicatorBuilder =
                    new TcpReplicatorBuilder(8087).heartBeatInterval(1, SECONDS);

            clusterReplicatorBuilder1 = new ClusterReplicatorBuilder((byte)
                    2, 1024);
            clusterReplicatorBuilder1.tcpReplicatorBuilder(tcpReplicatorBuilder);
            clusterB = clusterReplicatorBuilder1.create();

            // this is how you add maps after the custer is created
            map1b = clusterReplicatorBuilder1.create((short) 1,
                    ChronicleMapBuilder.of(Integer.class, CharSequence.class)
                            .entries(1000)
                            .file(getPersistenceFile()));

            map2b = clusterReplicatorBuilder1.create((short) 2,
                    ChronicleMapBuilder.of(Integer.class, CharSequence.class)
                            .entries(1000)
                            .file(getPersistenceFile()));
        }


    }

    @After
    public void tearDown() throws InterruptedException {

        for (final Closeable closeable : new Closeable[]{clusterA, clusterB}) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * sets up a 2 clusters ( notionally one for each host ), and replicates 2 Chronicle Maps between them
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void test() throws IOException, InterruptedException {

        map2a.put(1, "EXAMPLE-2");
        map1a.put(1, "EXAMPLE-1");

        // allow time for the recompilation to resolve
        waitTillEqual(2500);

        Assert.assertEquals("map1a=map1b", map1a, map1b);
        Assert.assertEquals("map2a=map2b", map2a, map2b);

        assertTrue("map1a.empty", !map1a.isEmpty());
        assertTrue("map2a.empty", !map2a.isEmpty());

    }


    /**
     * waits until map1 and map2 show the same value
     *
     * @param timeOutMs timeout in milliseconds
     * @throws InterruptedException
     */

    private void waitTillEqual(final int timeOutMs) throws InterruptedException {
        for (int t = 0; t < timeOutMs; t++) {
            if (map1a.equals(map1b) &&
                    map2a.equals(map2b))
                break;
            Thread.sleep(1);
        }

    }

}


