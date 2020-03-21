package com.km.service.ConfigureModule.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.km.data.common.util.Configuration;
import com.km.service.ConfigureModule.domain.Conf;
import com.km.service.ConfigureModule.dto.ConfUseridDto;
import com.km.service.ConfigureModule.service.ConfigureService;
import com.km.service.ProcessModule.domain.Process;
import com.km.service.ProcessModule.dto.ProcessUseridDto;
import com.km.service.UserModule.domain.User;
import com.km.utils.LoadConfigureUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
public class ConfigureController {

    @Autowired
    private ConfigureService configureService;

    @RequestMapping(value = "/getAllConfigures", method = RequestMethod.POST)
    public Object getAllConfigures(HttpServletRequest req) {
        int pageSize = Integer.parseInt(req.getParameter("pageSize"));
        int pageNumber = Integer.parseInt(req.getParameter("pageNumber"));
        List<ConfUseridDto> list = configureService.getAllConfigures(pageSize,pageNumber);
        int totalSize = configureService.getConfigureCount();
        int totalPages = totalSize/pageSize+(totalSize%pageSize==0?0:1);
        JSONObject message = new JSONObject();
        message.put("pageSize",pageSize);
        message.put("pageNumber",pageNumber);
        message.put("totalPages",totalPages);
        message.put("confDesc",list);
        return JSONObject.toJSON(message);
    }


    @RequestMapping(value = "/getOneConfigure", method = RequestMethod.POST)
    public Object getOneConfigure(HttpServletRequest req) {
        String configureId = req.getParameter("configureId");
        Conf conf = configureService.getConfigureByconfigureId(configureId);
        return JSONObject.toJSON(conf);
    }

    @RequestMapping(value = "/addConfigure", method = RequestMethod.POST)
    public Object addConfigure(HttpServletRequest req) {
        String configureName = req.getParameter("configureName");
        String configureContent = req.getParameter("configureContent");
        String configureType = req.getParameter("configureType");
        User user = (User) req.getAttribute("user");
        String userId = user.getUserId();
        configureService.addConfigure(configureType,configureName,configureContent,userId);
        JSONObject message = new JSONObject();
        message.put("message","新增配置文件成功");
        return JSONObject.toJSON(message);
    }

    @RequestMapping(value = "/deleteConfigure", method = RequestMethod.POST)
    public Object deleteConfigure(HttpServletRequest req) {
        String configureId = req.getParameter("configureId");
        configureService.deleteConfigure(configureId);
        JSONObject message = new JSONObject();
        message.put("message","删除配置文件成功");
        return JSONObject.toJSON(message);
    }

    @RequestMapping(value = "/updateConfigure", method = RequestMethod.POST)
    public Object updateConfigure(HttpServletRequest req) {
        String configureId = req.getParameter("configureId");
        String configureName = req.getParameter("configureName");
        String configureContent = req.getParameter("configureContent");
        configureService.updateConfigure(configureId,configureName,configureContent);
        JSONObject message = new JSONObject();
        message.put("message","更新配置文件成功");
        return JSONObject.toJSON(message);
    }
}