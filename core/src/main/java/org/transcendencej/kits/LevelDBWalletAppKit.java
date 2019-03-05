package org.transcendencej.kits;

import org.transcendencej.core.NetworkParameters;
import org.transcendencej.store.BlockStore;
import org.transcendencej.store.BlockStoreException;
import org.transcendencej.store.LevelDBBlockStore;
import org.transcendencej.store.SPVBlockStore;

import java.io.File;

/**
 * Created by Eric on 2/23/2016.
 */
public class LevelDBWalletAppKit extends WalletAppKit {
    public LevelDBWalletAppKit(NetworkParameters params, File directory, String filePrefix) {
        super(params, directory, filePrefix);
    }

    /**
     * Override this to use a {@link BlockStore} that isn't the default of {@link SPVBlockStore}.
     */
    protected BlockStore provideBlockStore(File file) throws BlockStoreException {
        return new LevelDBBlockStore(context, file);
    }
}
