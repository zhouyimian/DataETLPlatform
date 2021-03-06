package com.km.service.ProcessModule.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.km.data.common.util.Configuration;
import com.km.service.ConfigureModule.service.ConfigureService;
import com.km.service.ProcessModule.domain.Process;
import com.km.service.ProcessModule.dto.ProcessUseridDto;
import com.km.service.ProcessModule.service.ProcessService;
import com.km.service.UserModule.domain.User;
import com.km.service.common.exception.serviceException;
import com.km.utils.LoadConfigureUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
public class ProcessController {

    @Autowired
    private ProcessService processService;
    @Autowired
    private ConfigureService configureService;


    @RequestMapping(value = "/getAllProcess", method = RequestMethod.POST)
    public Object getAllProcess(HttpServletRequest req) {
        int pageSize = Integer.parseInt(req.getParameter("pageSize"));
        int pageNumber = Integer.parseInt(req.getParameter("pageNumber"));
        List<ProcessUseridDto> list = processService.getAllProcess(pageSize, pageNumber);
        JSONArray processDesc = richProcess(list);

        int totalSize = processService.getProcessCount();
        int totalPages = totalSize / pageSize + (totalSize % pageSize == 0 ? 0 : 1);
        JSONObject message = new JSONObject();
        message.put("pageSize", pageSize);
        message.put("pageNumber", pageNumber);
        message.put("totalPages", totalPages);
        message.put("processDesc", processDesc);
        return JSONObject.toJSON(message);
    }

    @RequestMapping(value = "/getAllPlugins", method = RequestMethod.POST)
    public Object getAllPlugins(HttpServletRequest req) {
        Map<String, Configuration> reader = LoadConfigureUtil.getReaderPlugNameToConf();
        Map<String, Configuration> writer = LoadConfigureUtil.getWriterPlugNameToConf();
        Map<String, Configuration> etl = LoadConfigureUtil.getEtlPlugNameToConf();
        JSONObject message = new JSONObject();
        message.put("readerPlugins", parseMap(reader));
        message.put("writerPlugins", parseMap(writer));
        message.put("etlPlugins", parseMap(etl));
        return JSONObject.toJSON(message);
    }


    @RequestMapping(value = "/getOneProcess", method = RequestMethod.POST)
    public Object getOneProcessDesc(HttpServletRequest req) {
        String processId = req.getParameter("processId");
        Process process = processService.getProcessByProcessId(processId);
        return JSONObject.toJSON(process);
    }

    @RequestMapping(value = "/addProcess", method = RequestMethod.POST)
    public Object addProcess(HttpServletRequest req) {
        String processName = req.getParameter("processName");
        String content = req.getParameter("processContent");
        String processContent = JSONObject.parseArray(content).toString();
        User user = (User) req.getAttribute("user");
        String userId = user.getUserId();
        processService.addProcess(processName, processContent, userId);
        JSONObject message = new JSONObject();
        message.put("message", "创建流程成功");
        return JSONObject.toJSON(message);
    }

    @RequestMapping(value = "/deleteProcess", method = RequestMethod.POST)
    public Object deleteProcess(HttpServletRequest req) {
        String processId = req.getParameter("processId");
        processService.deleteProcess(processId);
        JSONObject message = new JSONObject();
        message.put("message", "删除流程成功");
        return JSONObject.toJSON(message);
    }

    @RequestMapping(value = "/updateProcess", method = RequestMethod.POST)
    public Object updateProcess(HttpServletRequest req) {
        String processId = req.getParameter("processId");
        String processName = req.getParameter("processName");
        String content = req.getParameter("processContent");
        String processContent = JSONObject.parseArray(content).toString();
        processService.updateProcess(processId, processName, processContent);
        JSONObject message = new JSONObject();
        message.put("message", "更新流程成功");
        return JSONObject.toJSON(message);
    }

    /**
     * 导出流程
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "/exportProcess", method = RequestMethod.POST)
    public Object exportProcess(HttpServletRequest req) {
        String ids = req.getParameter("processIds");
        JSONArray processIds = JSONArray.parseArray(ids);
        JSONArray processJSONArray = new JSONArray();
        for (int i = 0; i < processIds.size(); i++) {
            Process process = processService.getProcessByProcessId(processIds.get(i).toString());
            JSONObject message = new JSONObject();
            message.put("processName", process.getProcessName());
            message.put("processContent", process.getProcessContent());
            processJSONArray.add(message);
        }
        JSONObject message = new JSONObject();
        message.put("contents", processJSONArray.toJSONString());
        return JSONObject.toJSON(message);
    }

    /**
     * 导入流程
     *
     * @param req
     * @return
     */
    @RequestMapping(value = "/importProcess", method = RequestMethod.POST)
    public Object importProcess(HttpServletRequest req) {
        String contents = req.getParameter("processes");
        JSONArray processContents = JSONArray.parseArray(contents);
        User user = (User) req.getAttribute("user");
        String userId = user.getUserId();
        for (int i = 0; i < processContents.size(); i++) {
            JSONObject oneProcess = processContents.getJSONObject(i);
            String processName = oneProcess.getString("processName");
            String processContent = oneProcess.getString("processContent");
            processService.addProcess(processName, processContent, userId);
        }
        JSONObject message = new JSONObject();
        message.put("message", "导入流程成功");
        return JSONObject.toJSON(message);
    }


    @RequestMapping(value = "/copyProcess", method = RequestMethod.POST)
    public Object copyProcess(HttpServletRequest req) {
        String processId = req.getParameter("processId");
        String newProcessName = req.getParameter("newProcessName");
        User user = (User) req.getAttribute("user");
        Process process = processService.getProcessByProcessId(processId);
        processService.addProcess(newProcessName, process.getProcessContent(), user.getUserId());
        JSONObject message = new JSONObject();
        message.put("message", "复制流程成功");
        return JSONObject.toJSON(message);
    }


    @RequestMapping(value = "/batchDeleteProcess", method = RequestMethod.POST)
    public Object batchDeleteProcess(HttpServletRequest req) {
        String ids = req.getParameter("processIds");
        User user = (User) req.getAttribute("user");
        JSONArray processIds = JSONArray.parseArray(ids);
        JSONObject message = new JSONObject();
        boolean isNotPermission = false;
        for (int i = 0; i < processIds.size(); i++) {
            Process process = processService.getProcessByProcessId(processIds.getString(i));
            if (!process.getUserId().equals(user.getUserId())) {
                isNotPermission = true;
                break;
            }
        }
        if (isNotPermission) {
            throw new serviceException("要删除的流程中有部分流程无删除权限!");
        } else {
            for (int i = 0; i < processIds.size(); i++)
                processService.deleteProcess(processIds.get(i).toString());
            message.put("message", "批量删除流程成功");
        }
        return JSONObject.toJSON(message);
    }


    private Object parseMap(Map<String, Configuration> configurationMap) {
        JSONArray array = new JSONArray();
        for (Map.Entry<String, Configuration> entry : configurationMap.entrySet()) {
            Configuration value = entry.getValue();
            array.add(JSONObject.parseObject(value.toJSON()));
        }
        return JSONObject.toJSON(array);
    }


    @RequestMapping(value = "/getAllPrivateProcess", method = RequestMethod.POST)
    public Object getAllPrivateProcess(HttpServletRequest req) {
        User user = (User) req.getAttribute("user");
        JSONObject message = new JSONObject();
        JSONArray processDesc;
        List<ProcessUseridDto> list;
        int pageSize;
        int pageNumber;
        int totalPages;
        if (req.getParameter("pageSize") == null && req.getParameter("pageNumber") == null) {
            list = processService.getAllPrivateProcess(user.getUserId());
            pageSize = list.size();
            pageNumber = 1;
            totalPages = 1;
        } else {
            pageSize = Integer.parseInt(req.getParameter("pageSize"));
            pageNumber = Integer.parseInt(req.getParameter("pageNumber"));
            list = processService.getPagePrivateProcess(user.getUserId(), pageSize, pageNumber);
            int totalSize = processService.getPrivateProcessCount(user.getUserId());
            totalPages = totalSize / pageSize + (totalSize % pageSize == 0 ? 0 : 1);
        }
        processDesc = richProcess(list);
        message.put("pageSize", pageSize);
        message.put("pageNumber", pageNumber);
        message.put("totalPages", totalPages);
        message.put("processDesc", processDesc);
        return JSONObject.toJSON(message);
    }

    private JSONArray richProcess(List<ProcessUseridDto> list) {
        JSONArray result = new JSONArray();
        for (ProcessUseridDto dto : list) {
            JSONObject object = JSONObject.parseObject(JSONObject.toJSON(dto).toString());
            JSONArray jsonArray = JSONArray.parseArray(dto.getProcessContent());
            int size = jsonArray.size();
            object.put("input", jsonArray.getJSONObject(0).getString("pluginName"));
            object.put("output", jsonArray.getJSONObject(size - 1).getString("pluginName"));
            result.add(object);
        }
        return result;
    }
}
