package com.orion.ops.framework.storage.core.client.local;

import com.orion.lang.utils.io.Files1;
import com.orion.lang.utils.io.Streams;
import com.orion.ops.framework.storage.core.client.AbstractFileClient;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * 本地文件客户端
 *
 * @author Jiahang Li
 * @version 1.0.0
 * @since 2023/6/30 17:21
 */
public class LocalFileClient extends AbstractFileClient<LocalFileClientConfig> {

    public LocalFileClient(LocalFileClientConfig config) {
        super(config);
    }

    @Override
    protected String doUpload(String path, InputStream in, boolean autoClose, boolean overrideIfExist) throws Exception {
        // 获取返回文件路径
        String returnPath = this.getReturnPath(path);
        // 检测文件是否存在
        if (!overrideIfExist && this.isExists(returnPath)) {
            return returnPath;
        }
        // 获取实际文件路径
        String absolutePath = this.getAbsolutePath(returnPath);
        // 上传文件
        try (OutputStream out = Files1.openOutputStreamFast(absolutePath)) {
            Streams.transfer(in, out);
        } finally {
            if (autoClose) {
                Streams.close(in);
            }
        }
        return returnPath;
    }

    @Override
    protected InputStream doDownload(String path) throws Exception {
        return Files1.openInputStreamFast(this.getAbsolutePath(path));
    }

    @Override
    public boolean isExists(String path) {
        return Files1.isFile(this.getAbsolutePath(path));
    }

    @Override
    public boolean delete(String path) {
        return Files1.delete(this.getAbsolutePath(path));
    }

    @Override
    protected String getReturnPath(String path) {
        // 拼接前缀
        return Files1.getPath(config.getBasePath() + "/" + this.getFilePath(path));
    }

    @Override
    protected String getAbsolutePath(String returnPath) {
        // 拼接存储路径
        return Files1.getPath(config.getStoragePath() + "/" + returnPath);
    }

}