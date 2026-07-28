package de.konavigator.app.data.remote.provider.deutscheboerse

import java.io.File
import java.io.FileInputStream

object DeutscheBoerseCompressedFileSnapshotProviderFactory {

    fun create(
        dxscGzipFile: File,
        xfraZipFile: File,
        requestedProductIsins: Set<String>
    ): DeutscheBoerseSnapshotProviderCreationResult =
        DeutscheBoerseSnapshotProviderFactory.createFromCompressedSources(
            openDxscCompressedInput = {
                FileInputStream(dxscGzipFile)
            },
            openXfraCompressedInput = {
                FileInputStream(xfraZipFile)
            },
            requestedProductIsins = requestedProductIsins
        )
}
