package net.liplum.animations.anims;


public interface IFrameIndexer {
    /**
     * Gets the index of the current frame.
     *
     * @param length the length of all frames
     * @return the index.If it has no frame or don't want to show any image, return -1.
     */
    int getCurIndex(int length);
}
