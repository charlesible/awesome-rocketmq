package org.apache.rocketmq.example.byteBuffer;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @author lichundong
 * @date 2019/08/28
 */
public class Test {
    public static void main(String[] args) {
        ByteBuffer bbuf = ByteBuffer.allocate(10);
        int capacity = bbuf.capacity(); // 10
        System.out.println(bbuf.position());
        System.out.println(bbuf.limit());


        System.out.println(capacity);
        bbuf.putShort( (short) 10);
        System.out.println(bbuf.position());
        bbuf.putShort( (short) 20);
        System.out.println(bbuf.position());
        bbuf.putShort( (short) 30);
        System.out.println(bbuf.position());

        System.out.println(bbuf.toString());

        System.out.println(Arrays.toString(bbuf.array()));

        bbuf.position(8);
        ByteBuffer bb = bbuf.slice();
        System.out.println(bb.position());
        bb.putShort((short) 40);

        System.out.println(bbuf.toString());
        System.out.println(Arrays.toString(bb.array()));
    }
}
