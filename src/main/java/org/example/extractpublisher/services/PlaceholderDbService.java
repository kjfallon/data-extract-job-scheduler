package org.example.extractpublisher.services;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.example.extractpublisher.entities.ItemSummary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Data
@Slf4j
@Service
public class PlaceholderDbService {

    private String DBdriver;
    private String DBurl;
    private String DBuser;
    private String DBpassword;
    private String DBschema;

    // Simulate a data extract
    public List<ItemSummary> queryDataExtract(String integrationPartner) {

        List<ItemSummary> itemList = new ArrayList<ItemSummary>();

        // Instead of querying data store, return dummy data
        for(int i=1; i<11; i++) {
            ItemSummary item = new ItemSummary();
            item.setAttribute1("value1");
            item.setAttribute2("value2");
            item.setAttribute3("value3");
            item.setAttribute4("value4");
            item.setAttribute5("value5");
            item.setAttribute6("value6");
            item.setAttribute7("value7");
            item.setAttribute8("value8");
            item.setAttribute9("value9");
            item.setAttribute10("value10");

            itemList.add(item);
        }

        return itemList;
    }

}
