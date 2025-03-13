"use client"
import {FaCamera, FaSearch} from "react-icons/fa";
import {BiSolidMicrophone} from "react-icons/bi";
import useSearchStore from "@/store/searchStore";
import {useRouter, useSearchParams} from "next/navigation";
import {twMerge} from 'tailwind-merge'
import {useLayoutEffect} from "react";
import {X} from "lucide-react";

export function SearchBar({variant = "home", ...props}) {
    const {query, setQuery} = useSearchStore();
    const searchParams = useSearchParams();
    const {push} = useRouter();

    const onSearch = () => {
        const params = new URLSearchParams(searchParams);
        if (query) {
            params.set('q', query);
        } else {
            params.delete('q');
        }
        push(`/search?${params.toString()}`);
    }

    const handleSearch = (e) => {
        if (e.key === 'Enter' && onSearch) {
            onSearch();
        }
    };

    useLayoutEffect(() => {
        setQuery(searchParams.get("q"));
    }, [])

    return (<div className={twMerge("flex w-full", props.className)}>
        <div
            className="relative flex items-center mx-2 h-[2em] w-auto flex-grow max-w-[40vw] min-w-[30vw] rounded-2xl bg-white shadow-md">

            {/* Search Icon conditionally rendered based on variant */}
            {variant === "home" && (<FaSearch className="absolute left-3 text-gray-500" size={13} />)}

            {/* Input Field - adjust padding based on variant */}
            <input
                value={query}
                onChange={event => (setQuery(event.target.value), console.log(event.target.value))}
                onKeyDown={handleSearch}
                className={twMerge("w-full h-full rounded-2xl outline-none text-black bg-transparent font-medium text-xs", variant === "home" ? "pl-10 pr-16" : "pl-4 pr-20")}
            />

            {/* Right Icons - Microphone, Camera & conditionally Search */}
            <div className="absolute right-3 flex space-x-2 items-center">
                {variant === "search" && (<>
                    <X className="text-gray-500" size={18} onClick={() => setQuery("")} />
                    <div className="h-5 border-l-[0.1vw] border-gray-300"></div>
                </>)}
                <BiSolidMicrophone className="text-gray-500" />
                <FaCamera className="text-gray-500" />
                {variant === "search" && (<>
                    <FaSearch className="text-gray-500" size={13} />
                </>)}


            </div>
        </div>
    </div>);
}