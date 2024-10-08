/**
 * Copyright 2024 IBM Corp. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.ibm.eventautomation.demos.loosehangerjeans.generators;

import com.ibm.eventautomation.demos.loosehangerjeans.DatagenSourceConfig;
import com.ibm.eventautomation.demos.loosehangerjeans.data.OnlineOrder;
import com.ibm.eventautomation.demos.loosehangerjeans.data.OutOfStock;
import com.ibm.eventautomation.demos.loosehangerjeans.data.Product;
import com.ibm.eventautomation.demos.loosehangerjeans.utils.Generators;
import org.apache.kafka.common.config.AbstractConfig;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Generates an {@link OutOfStock} event for a given product using randomly generated data.
 */
public class OutOfStockGenerator extends Generator<OutOfStock> {

    /**
     * Minimum time (in days) between the time that the product was out-of-stock and the restocking date.
     */
    private final int restockingMinDelay;

    /**
     * Maximum time (in days) between the time that the product was out-of-stock and the restocking date.
     */
    private final int restockingMaxDelay;

    /** Creates an {@link OutOfStockGenerator} using the provided configuration. */
    public OutOfStockGenerator(AbstractConfig config) {
        super(config.getInt(DatagenSourceConfig.CONFIG_TIMES_ONLINEORDERS),
              config.getInt(DatagenSourceConfig.CONFIG_DELAYS_OUTOFSTOCKS),
              config.getDouble(DatagenSourceConfig.CONFIG_DUPLICATE_OUTOFSTOCKS),
              config.getString(DatagenSourceConfig.CONFIG_FORMATS_TIMESTAMPS));

        this.restockingMinDelay = config.getInt(DatagenSourceConfig.CONFIG_OUTOFSTOCKS_RESTOCKING_MIN_DELAY);
        this.restockingMaxDelay = config.getInt(DatagenSourceConfig.CONFIG_OUTOFSTOCKS_RESTOCKING_MAX_DELAY);
    }

    public OutOfStock generate(OnlineOrder order) {
        // Retrieve a product description randomly in the order.
        List<String> products = order.getProducts();
        String productDescription = Generators.randomItem(products);

        // Generate a product object from the description
        Product product = Product.parseDescription(productDescription);
        if (product == null) {
            return null;
        }

        // Generate an out-of-stock event for the product
        ZonedDateTime dateTime = Generators.nowWithRandomOffset(MAX_DELAY_SECS);
        long timestamp = dateTime.toInstant().toEpochMilli();
        int restockingDelay = Generators.randomInt(restockingMinDelay, restockingMaxDelay);
        int restockingDate = (int) dateTime.plusDays(restockingDelay).toLocalDate().toEpochDay();
        return new OutOfStock(timestamp, product, restockingDate, dateTime);
    }

    @Override
    protected OutOfStock generateEvent(ZonedDateTime timestamp) {
        throw new UnsupportedOperationException("Out of stock notifications cannot be generated without a product to notify about");
    }
}
