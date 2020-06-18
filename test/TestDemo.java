import lombok.Getter;
import lombok.Setter;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author Cherry
 * @Date 2020/6/1
 * @Time 19:59
 * @Brief
 */

public class TestDemo {

    private int tomato = 0;         //西红柿
    private int cucumber = 0;       //黄瓜
    private int carob = 0;          //豆角
    private int peopleNum = 0;      //拼团人数
    private List<String> place = new ArrayList<>();
    private String comInfo = "";    //活动简介

    @Test
    public void test() {
        peopleNum = 19;
        tomato = -2;
        cucumber = 101;
        carob = 0;
        place.add("地址1");
        place.add("地址2");
        place.add("地址3");
    }
}

/**
 * 商家类，存放商家等信息
 */
@Getter
@Setter
class Seller {
    private String name = "";       //商家名称
    private String address = "";    //商家地址
    private String tel = "";        //联系电话
    private String info = "";       //商家简介
}
