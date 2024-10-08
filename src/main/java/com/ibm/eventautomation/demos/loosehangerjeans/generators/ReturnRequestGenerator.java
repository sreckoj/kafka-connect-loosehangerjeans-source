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
import com.ibm.eventautomation.demos.loosehangerjeans.data.Address;
import com.ibm.eventautomation.demos.loosehangerjeans.data.Country;
import com.ibm.eventautomation.demos.loosehangerjeans.data.NamedAddress;
import com.ibm.eventautomation.demos.loosehangerjeans.data.OnlineCustomer;
import com.ibm.eventautomation.demos.loosehangerjeans.data.Product;
import com.ibm.eventautomation.demos.loosehangerjeans.data.ProductReturn;
import com.ibm.eventautomation.demos.loosehangerjeans.data.ReturnRequest;
import com.ibm.eventautomation.demos.loosehangerjeans.utils.Generators;
import org.apache.kafka.common.config.AbstractConfig;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates a {@link ReturnRequest} event using randomly generated data.
 */
public class ReturnRequestGenerator extends Generator<ReturnRequest> {

    /** Helper class to randomly generate the details of a product. */
    private final ProductGenerator productGenerator;

    /**
     * Ratio of products in a return request that have a size issue.
     * Must be between 0.0 and 1.0.
     *
     * Setting this to 0 will mean that no product has a size issue in a given return request.
     * Setting this to 1 will mean that all the products have a size issue in a given return request.
     */
    private final double productWithSizeIssueRatio;

    /** Products with a size issue will be chosen from this list. */
    private final List<Product> productsWithSizeIssue;

    /** Minimum number of products to include in the return request. */
    private final int minProducts;

    /** Maximum number of products to include in the return request. */
    private final int maxProducts;

    /** Minimum quantity for a returned product. */
    private final int minQuantity;

    /** Maximum quantity for a returned product. */
    private final int maxQuantity;

    /** Reasons for returning a product will be chosen from this list. */
    private final List<String> reasons;

    /** Minimum number of emails for the customer who makes the return request. */
    private final int minEmails;

    /** Maximum number of emails for the customer who makes the return request. */
    private final int maxEmails;

    /** Minimum number of phones in an address used for the order. */
    private final int minPhones;

    /** Maximum number of phones in an address used for the order. */
    private final int maxPhones;

    /**
     * Ratio of orders that use the same address as shipping and billing address.
     *
     * Between 0.0 and 1.0.
     *
     * Setting this to 0 will mean no events will use the same address as shipping and billing address.
     * Setting this to 1 will mean every event uses the same address as shipping and billing address.
     */
    private final double reuseAddressRatio;

    /**
     * Ratio of return requests that have at least one product that has a review that is
     * posted after the return request is issued.
     * Must be between 0.0 and 1.0.
     *
     * Setting this to 0 will mean that no product review event is generated.
     * Setting this to 1 will mean that one product review event will be generated for each
     *  new return request.
     */
    private final double reviewRatio;


    /** Creates an {@link ReturnRequestGenerator} using the provided configuration. */
    public ReturnRequestGenerator(AbstractConfig config,
                                  List<Product> productsWithSizeIssue)
    {
        super(config.getInt(DatagenSourceConfig.CONFIG_TIMES_RETURNREQUESTS),
              config.getInt(DatagenSourceConfig.CONFIG_DELAYS_RETURNREQUESTS),
              config.getDouble(DatagenSourceConfig.CONFIG_DUPLICATE_RETURNREQUESTS),
              config.getString(DatagenSourceConfig.CONFIG_FORMATS_TIMESTAMPS_LTZ));

        this.productGenerator = new ProductGenerator(config);

        this.productsWithSizeIssue = productsWithSizeIssue;

        this.productWithSizeIssueRatio = config.getDouble(DatagenSourceConfig.CONFIG_RETURNREQUESTS_PRODUCT_WITH_SIZE_ISSUE_RATIO);

        this.minProducts = config.getInt(DatagenSourceConfig.CONFIG_RETURNREQUESTS_PRODUCTS_MIN);
        this.maxProducts = config.getInt(DatagenSourceConfig.CONFIG_RETURNREQUESTS_PRODUCTS_MAX);

        this.minQuantity = config.getInt(DatagenSourceConfig.CONFIG_RETURNREQUESTS_PRODUCT_QUANTITY_MIN);
        this.maxQuantity = config.getInt(DatagenSourceConfig.CONFIG_RETURNREQUESTS_PRODUCT_QUANTITY_MAX);

        this.reasons = config.getList(DatagenSourceConfig.CONFIG_RETURNREQUESTS_REASONS);

        this.minEmails = config.getInt(DatagenSourceConfig.CONFIG_RETURNREQUESTS_CUSTOMER_EMAILS_MIN);
        this.maxEmails = config.getInt(DatagenSourceConfig.CONFIG_RETURNREQUESTS_CUSTOMER_EMAILS_MAX);

        this.minPhones = config.getInt(DatagenSourceConfig.CONFIG_RETURNREQUESTS_ADDRESS_PHONES_MIN);
        this.maxPhones = config.getInt(DatagenSourceConfig.CONFIG_RETURNREQUESTS_ADDRESS_PHONES_MAX);

        this.reuseAddressRatio = config.getDouble(DatagenSourceConfig.CONFIG_RETURNREQUESTS_REUSE_ADDRESS_RATIO);
        this.reviewRatio = config.getDouble(DatagenSourceConfig.CONFIG_RETURNREQUESTS_REVIEW_RATIO);
    }

    @Override
    protected ReturnRequest generateEvent(ZonedDateTime timestamp) {
        // Generate a random customer.
        OnlineCustomer customer = OnlineCustomer.create(faker, minEmails, maxEmails);

        // Generate the country for the addresses.
        Country country = new Country(DEFAULT_LOCALE.getCountry(), DEFAULT_LOCALE.getDisplayCountry(DEFAULT_LOCALE));

        // Generate a random billing address.
        Address billingAddress = Address.create(faker, country, minPhones, maxPhones);

        List<NamedAddress> addresses = new ArrayList<>();
        // Add the billing address to the addresses.
        addresses.add(NamedAddress.create("Billing address", billingAddress));

        // A shipping address is added to the addresses only if we should not reuse the address
        // used as billing address.
        if (!Generators.shouldDo(reuseAddressRatio)) {
            // Generate a random shipping address that is different from the billing address.
            Address shippingAddress = Address.create(faker, country, minPhones, maxPhones);
            // Add the shipping address to the addresses.
            addresses.add(NamedAddress.create("Shipping address", shippingAddress));
        }

        // Generate some product returns randomly.
        int productCount = Generators.randomInt(minProducts, maxProducts);
        List<ProductReturn> returns = new ArrayList<>();
        for (int i = 0; i < productCount; i++) {
            int quantity = Generators.randomInt(minQuantity, maxQuantity);
            Product product = Generators.shouldDo(productWithSizeIssueRatio)
                    ? Generators.randomItem(productsWithSizeIssue)
                    : productGenerator.generate();
            returns.add(new ProductReturn(product, quantity, Generators.randomItem(reasons)));
        }

        return new ReturnRequest(formatTimestamp(timestamp),
                                 customer,
                                 addresses,
                                 returns,
                                 timestamp);
    }

    /**
     * Returns a random decision of whether an return request should be
     *  followed by a review.
     *
     *  The frequency for how often this returns true is determined by
     *  a ratio provided to the generator constructor.
     *
     * @return true if a review should be generated
     */
    public boolean shouldReview() {
        return Generators.shouldDo(reviewRatio);
    }
}
