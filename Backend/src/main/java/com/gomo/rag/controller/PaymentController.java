package com.gomo.rag.controller;

import com.gomo.rag.service.PaymentService;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(value = "*")
@RequestMapping("/pay")
public class PaymentController {   // testing PAYMENT GATEWAY API


    @Autowired
    private PaymentService paymentService;


    @PostMapping("/order")
    public String createOrder(@RequestParam int amount , @RequestParam String currency ) throws RazorpayException {

        try{

        return paymentService.createOrder(amount,currency);}
        catch (Exception e){
            System.out.println(e.getLocalizedMessage());
        }

        return "done";
    }


}
