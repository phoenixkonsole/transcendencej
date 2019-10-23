/*
 * Copyright 2011 John Sample
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.transcendencej.net.discovery;

import org.transcendencej.core.*;
import org.transcendencej.utils.*;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

/**
 * <p>Supports peer discovery through DNS.</p>
 *
 * <p>Failure to resolve individual host names will not cause an Exception to be thrown.
 * However, if all hosts passed fail to resolve a PeerDiscoveryException will be thrown during getPeers().
 * </p>
 *
 * <p>DNS seeds do not attempt to enumerate every peer on the network. {@link DnsDiscovery#getPeers(long, java.util.concurrent.TimeUnit)}
 * will return up to 30 random peers from the set of those returned within the timeout period. If you want more peers
 * to connect to, you need to discover them via other means (like addr broadcasts).</p>
 */
public class DnsDiscovery extends MultiplexingDiscovery {
    /**
     * Supports finding peers through DNS A records. Community run DNS entry points will be used.
     *
     * @param netParams Network parameters to be used for port information.
     */
    public DnsDiscovery(NetworkParameters netParams) {
        this(netParams.getDnsSeeds(), netParams);
    }

    /**
     * Supports finding peers through DNS A records.
     *
     * @param dnsSeeds Host names to be examined for seed addresses.
     * @param params Network parameters to be used for port information.
     */
    public DnsDiscovery(String[] dnsSeeds, NetworkParameters params) {
        super(params, buildDiscoveries(params, dnsSeeds));
    }

    private static List<PeerDiscovery> buildDiscoveries(NetworkParameters params, String[] seeds) {
        List<PeerDiscovery> discoveries = new ArrayList<PeerDiscovery>();
        if (seeds != null)
            for (String seed : seeds)
                discoveries.add(new DnsSeedDiscovery(params, seed));
        return discoveries;
    }

    @Override
    protected ExecutorService createExecutor() {
        // Attempted workaround for reported bugs on Linux in which gethostbyname does not appear to be properly
        // thread safe and can cause segfaults on some libc versions.
        if (System.getProperty("os.name").toLowerCase().contains("linux"))
            return Executors.newSingleThreadExecutor(new ContextPropagatingThreadFactory("DNS seed lookups"));
        else
            return Executors.newFixedThreadPool(seeds.size(), new DaemonThreadFactory("DNS seed lookups"));
    }

    /** Implements discovery from a single DNS host. */
    public static class DnsSeedDiscovery implements PeerDiscovery {
        private final String hostname;
        private final NetworkParameters params;

        public DnsSeedDiscovery(NetworkParameters params, String hostname) {
            this.hostname = hostname;
            this.params = params;
        }

        @Override
        public InetSocketAddress[] getPeers(long services, long timeoutValue, TimeUnit timeoutUnit) throws PeerDiscoveryException {
            if (services != 0)
                throw new PeerDiscoveryException("DNS seeds cannot filter by services: " + services);
            try {
                SimpleResolver resolver = new SimpleResolver(hostname);
                Lookup lookup = new Lookup(hostname);
                lookup.setResolver(resolver);
                Record[] records = lookup.run();
                InetSocketAddress[] results = new InetSocketAddress[records.length];
                for (int i = 0; i < records.length; i++) {
                    if (records[i].getType() == Type.A) {
                        results[i] = new InetSocketAddress(((ARecord)records[i]).getAddress(), params.getPort());
                    }
                }
                return results;
            } catch (UnknownHostException e) {
                throw new PeerDiscoveryException(e);
            } catch (TextParseException e) {
                throw new PeerDiscoveryException(e);
            }
        }

        @Override
        public void shutdown() {
        }

        @Override
        public String toString() {
            return hostname;
        }
    }
}
