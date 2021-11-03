/*********************************************************************
 * Copyright (c) 2018-2021 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
'use strict';

import {TelemetryApi, TelemetryClient} from '../src';
import * as mockAxios from 'axios';
const axios = (mockAxios as any);

describe('RestAPI >', () => {
    let telemetryClient: TelemetryApi;

    beforeEach(() => {
    	telemetryClient = new TelemetryClient();
        jest.resetAllMocks();
    });

    it('activity test - successful', async () => {
        axios.request.mockImplementationOnce(() =>
        Promise.resolve({
            status: 200,
            responseText: ''
        })
        );
        await telemetryClient.activity({userId : "userId" })
        expect(axios.request).toHaveBeenCalled();
        const call = (axios.request as jest.Mock).mock.calls[0][0] as any;
        expect(call.method).toBe('POST');
        expect(call.url).toBe('/telemetry/activity');
    });

    it('activity test - Error', async () => {
        axios.request.mockImplementationOnce(() => {throw new Error('error')});
        await expect(telemetryClient.activity({userId : "userId" })).rejects.toThrow('');
        const call = (axios.request as jest.Mock).mock.calls[0][0] as any;
        expect(call.method).toBe('POST');
        expect(call.url).toBe('/telemetry/activity');
    });
    
});
