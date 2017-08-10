package io.elastest.ece.application;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.elastest.ece.model.CostModel;
import io.elastest.ece.model.TJob;
import io.elastest.ece.persistance.HibernateClient;
import io.elastest.ece.persistance.QueryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2017. Zuercher Hochschule fuer Angewandte Wissenschaften
 * All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 * <p>
 * Created by Manu Perez on 21/06/17.
 */

@Controller
public class APIController {

    private static final Logger logger = LoggerFactory.getLogger(APIController.class.getName());

    @PostConstruct
    public void init() {
//        testCostModelValues();
//        testTestValues();
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String getIndex(Model model) {
        logger.info("Creating test values.");
        HibernateClient hibernateClient = HibernateClient.getInstance();
        List<CostModel> costModels = hibernateClient.executeQuery(QueryHelper.createListQuery(CostModel.class));
        List<TJob> tJobs = hibernateClient.executeQuery(QueryHelper.createListQuery(TJob.class));

        logger.info("Adding attributes to the model.");
        model.addAttribute("tests", tJobs);
        model.addAttribute("costModels", costModels);
        logger.info("Redirecting to the ECE's Index Page.");
        return "index";
    }

    @RequestMapping(value = "/costmodel", method = RequestMethod.POST)
    public String addCostModel(@RequestParam String name, @RequestParam String description, @RequestParam String fixName, @RequestParam Double fixValue, @RequestParam double cpus, @RequestParam double memory, @RequestParam double disk, Model model) {
        logger.info("Creating Cost Model.");
        HashMap<String, Double> varCosts = new HashMap<>();
        varCosts.put("cpus", cpus);
        varCosts.put("memory", memory);
        varCosts.put("disk", disk);

        HashMap<String, Double> fixCosts = new HashMap<>();
        fixCosts.put(fixName, fixValue);


        CostModel costModel = new CostModel(name, "ONDEMAND", fixCosts, varCosts, null, description);
        logger.info("Persisting the created Cost Model into the DB");
        HibernateClient hibernateClient = HibernateClient.getInstance();
        hibernateClient.persistObject(costModel);
        logger.info("Added a new Cost Model.");
        return "costModelCreated";
    }

    @RequestMapping(value = "/costmodel", method = RequestMethod.GET)
    public String getCostModel(@RequestParam String costModelId, Model model) {
        logger.info("Returning information about the cost model: " + costModelId);
        HibernateClient hibernateClient = HibernateClient.getInstance();
        CostModel costModel = (CostModel) hibernateClient.getObject(CostModel.class, Long.valueOf(costModelId));
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        model.addAttribute("costModel", gson.toJson(costModel));
        return "costModelInfo";
    }

    @RequestMapping(value = "/deleteCostModel", method = RequestMethod.POST)
    public String deleteCostModel(@RequestParam String costModelId) {
        logger.info("Deleting the Cost Model: " + costModelId);
        HibernateClient hibernateClient = HibernateClient.getInstance();
        CostModel costModel = (CostModel) hibernateClient.getObject(CostModel.class, Long.valueOf(costModelId));
        hibernateClient.deleteObject(costModel);
        logger.info("Deleted.");
        return "costModelDeleted";
    }

    @RequestMapping(value = "/estimate", method = RequestMethod.POST)
    public String estimatePost(@RequestParam String testId, @RequestParam String costModelId, Model model) {
        HibernateClient hibernateClient = HibernateClient.getInstance();
        CostModel costModel = (CostModel) hibernateClient.getObject(CostModel.class, Long.valueOf(costModelId));
        TJob tJob = (TJob) hibernateClient.getObject(TJob.class, Long.valueOf(testId));
        double totalCost = 0;
        String costModelType = costModel.getType();
        if(costModelType.equalsIgnoreCase("ONDEMAND")){
            Map<String, Double> fixCost = costModel.getFix_cost();
            Map<String, Double> varRate = costModel.getVar_rate();
            Map<String, String> components = costModel.getComponents();
            Map<String, String> metadata = tJob.getMetadata();

            logger.info("Adding all fix costs from the cost model.");
            for(Object key : fixCost.keySet()){
                totalCost += fixCost.get(key);
            }

            logger.info("Adding all the accounted variable costs in the model");
            for(Object key : varRate.keySet()){
                if(tJob.getMetadata().containsKey(key)){
                    totalCost += (Double.parseDouble(metadata.get(key)) * (varRate.get(key)));
                }
            }

            //TODO:add component costs.
            logger.info("Adding all the component costs.");
        }else if(costModelType.equalsIgnoreCase("PAYG")){
            //TODO: implement
        }

        model.addAttribute("estimate", totalCost);
        logger.info("Returning an estimate for the test jobs: " + testId + " and the Cost Model: " + costModelId);
        return "estimate";
    }

    private ArrayList testCostModelValues() {
        logger.info("Creating Demo Cost Model Values.");
        ArrayList<CostModel> result = new ArrayList();
        HashMap varCosts = new HashMap();
        varCosts.put("cpus", 50.0);
        varCosts.put("memory", 10.0);
        varCosts.put("disk", 1.0);
        HashMap fixCost = new HashMap();
        fixCost.put("deployment", 5.0);

        CostModel costModel = new CostModel("On Demand 5 + Charges", "ONDEMAND", fixCost, varCosts, null, "On Demand 5 per deployment, 50 per core, 10 per GB ram and 1 per GB disk");
        result.add(costModel);

        HashMap varCosts1 = new HashMap();
        varCosts1.put("cpus", 1.0);
        varCosts1.put("memory", 1.0);
        varCosts1.put("disk", 1.0);
        HashMap fixCost1 = new HashMap();
        fixCost1.put("deployment", 10.0);

        CostModel costModel1 = new CostModel("On demand 10 + Charges", "ONDEMAND", fixCost1, varCosts1, null, "On Demand 10 per deployment, 1 per core, 1 per GB ram and 1 per GB disk");
        result.add(costModel1);

        logger.info("Persisting demo values");
        HibernateClient hibernateClient = HibernateClient.getInstance();
        hibernateClient.persistObject(costModel);
        hibernateClient.persistObject(costModel1);
        return result;
    }

    private ArrayList testTestValues() {
        logger.info("Creating Demo T-Job Values.");
        ArrayList<TJob> result = new ArrayList();
        HashMap<String, String> test0Map = new HashMap<>();
        test0Map.put("cpus", "8.0");
        test0Map.put("memory", "20.0");
        test0Map.put("disk", "500.0");

        HashMap<String, String> test1Map = new HashMap<>();
        test1Map.put("cpus", "1.0");
        test1Map.put("memory", "1.0");
        test1Map.put("disk", "1.0");

        TJob tJob = new TJob("test0 8 cores, 20gb ram, 500gb disk ", test0Map);
        TJob tJob1 = new TJob("test1 1 core, 1gb ram, 1gb disk ", test1Map);
        tJob.setDescription("Test using a Big VM");

        result.add(tJob);
        result.add(tJob1);

        logger.info("Persisting demo values");
        HibernateClient hibernateClient = HibernateClient.getInstance();
        hibernateClient.persistObject(tJob);
        hibernateClient.persistObject(tJob1);
        return result;
    }
}
