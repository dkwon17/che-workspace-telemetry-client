/*********************************************************************
 * Copyright (c) 2018 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

import axios, {AxiosInstance, AxiosPromise, AxiosResponse, AxiosError, AxiosRequestConfig} from 'axios';
import {DefaultApiFactory, DefaultApiInterface, Event} from './openapi/api';
import {ConfigurationParameters, Configuration} from './openapi/configuration';
export * from './openapi/configuration';
export * from './openapi/api';

type AxiosPromiseWrapper<T> = {
    [K in keyof T]: T[K] extends (...args: infer Args) => AxiosPromise<infer R> ? (...args: Args) => Promise<R> : T[K];
};

export interface TelemetryApi extends AxiosPromiseWrapper<DefaultApiInterface> {}

export class TelemetryClient implements TelemetryApi {
    private delegate : DefaultApiInterface

    constructor(conf? : Configuration, basePath: string = '', axiosInstance: AxiosInstance = axios) {
        this.delegate = DefaultApiFactory(conf, basePath, axiosInstance);
    }

    private wrapInPromise<T, Args extends any[]>(f: (...args: Args) => AxiosPromise<string>, ...args: Args) {
        return new Promise<string>((resolve, reject) => {
            f(...args)
                .then(() => {
                    resolve('');
                })
                .catch((error: AxiosError) => {
                    reject(new RequestError(error));
                });
        });
    }

    activity(options?: any): Promise<string> {
        return this.wrapInPromise(this.delegate.activity, options);
    }

    event(event: Event, options?: any): Promise<string> {
        return this.wrapInPromise(this.delegate.event, event);
    }
}

export interface IRequestConfig extends AxiosRequestConfig { }

export interface IResponse<T> extends AxiosResponse<T> {
    data: T;
    status: number;
    statusText: string;
    headers: any;
    config: IRequestConfig;
    request?: any;
}

export interface IRequestError extends Error {
    status?: number;
    config: AxiosRequestConfig;
    request?: any;
    response?: IResponse<any>;
}

class RequestError implements IRequestError {

    status: number | undefined;
    name: string;
    message: string;
    config: AxiosRequestConfig;
    request: any;
    response: AxiosResponse | undefined;

    constructor(error: AxiosError) {
        if (error.code) {
            this.status = Number(error.code);
        }
        this.name = error.name;
        this.message = error.message;
        this.config = error.config;
        if (error.request) {
            this.request = error.request;
        }
        if (error.response) {
            this.response = error.response;
        }
    }
}
